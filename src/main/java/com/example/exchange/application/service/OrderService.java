/*
 * 檔案用途：應用服務，編排領域模型、Repository 與外部基礎設施完成業務流程。
 */
package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.dto.FeeCalculation;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.model.entity.SymbolConfig;
import com.example.exchange.domain.model.dto.PositionChange;
import com.example.exchange.domain.repository.EventStore;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBookSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OrderService（訂單 / 成交服務）
 * -------------------------------------------------
 *
 * 角色定位：
 * - 此服務位於 Application Service 層，負責串接：
 *   1) 訂單送入撮合引擎
 *   2) 成交事件寫入 EventStore
 *   3) 倉位（Position）更新
 *   4) 成交事件往外發布
 *   5) 訂單狀態回寫到 OrderRepository
 *
 * 設計重點：
 * - 撮合引擎只負責撮合，不負責持久化與帳務
 * - OrderService 負責把「撮合結果」落到系統狀態中
 * - 串起撮合 → 事件 → 持倉/帳務 → 行情 → 訂單回寫
 *
 * 修正重點：
 * 1) 不再使用舊版 submitOrder()，統一改走 matchingEngine.submit(order)
 * 2) 使用 MatchingResult 取得：
 *    - trades：成交事件列表
 *    - affectedOrders：所有受影響訂單
 * 3) 回寫所有受影響訂單，而不只是新單本身
 *
 * 後續可擴充：
 * - funding accrual / funding settlement
 * - liquidation / insurance fund / ADL
 * - 訂單事件（OrderCreated / OrderUpdated / OrderCanceled / OrderRejected）
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    /**
     * 撮合引擎
     * - 負責把新訂單送進訂單簿並執行撮合
     */
    private final MatchingEngine matchingEngine;

    /**
     * 倉位儲存庫
     * - 用於查詢與保存使用者在各交易對上的持倉
     */
    private final PositionRepository posRepo;

    /**
     * 事件儲存
     * - 用於保存可回放的成交事件
     */
    private final EventStore eventStore;

    /**
     * 領域事件發布器
     * - 目前主要把成交事件往外送（例如 Kafka）
     */
    private final DomainEventPublisher<Object> publisher;

    /**
     * 訂單儲存庫
     * - 用於保存訂單狀態，供查詢掛單 / 歷史單
     */
    private final OrderRepository orderRepo;
    private final SymbolConfigRepository symbolConfigRepository;
    private final WalletLedgerService walletLedgerService;
    private final FeeService feeService;
    private final RiskService riskService;
    private final MarketDataService marketDataService;
    private final IdempotencyService idempotencyService;

    /**
     * 處理新訂單
     * -------------------------------------------------
     * 主流程：
     * 1) 將訂單送入撮合引擎
     * 2) 取得撮合結果（成交事件 + 受影響訂單）
     * 3) 逐筆成交事件：
     *    - 寫入 EventStore
     *    - 更新 Position
     *    - 發布事件
     * 4) 回寫所有受影響訂單
     * 5) 預留後續帳務 / 手續費 / 資金費 / 強平出入點
     *
     * @param order 新訂單
     */
    public void processOrder(Order order) {
        publisher.publish(OrderLifecycleEvent.accepted(order));

        // 1) 送入撮合引擎，取得撮合結果
        MatchingResult result = matchingEngine.submit(order);

        // 本次撮合產生的成交事件
        List<TradeExecuted> trades = result.getTrades();

        // 本次撮合中所有受影響的訂單（新單 + 對手單）
        List<Order> affectedOrders = result.getAffectedOrders();
        Map<UUID, Order> affectedById = affectedOrders.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Order::getId, affected -> affected, (left, right) -> left));

        // 2) 逐筆成交：事件入庫 → 更新倉位 → 發布事件
        for (TradeExecuted trade : trades) {
            // 將成交事件寫入 EventStore，取得單調序號
            long seq = eventStore.append(trade);

            // 回填 seq，作為往外發布或後續 replay 用事件
            TradeExecuted withSeq = trade.withSeq(seq);
            if (!idempotencyService.markProcessed(withSeq.idempotencyKey())) {
                continue;
            }
            Order relatedOrder = trade.orderId() == null ? null : affectedById.get(trade.orderId());
            SymbolConfig config = symbolConfigRepository.findBySymbol(trade.symbol().code())
                    .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + trade.symbol().code()));

            // 查詢或建立當前持倉
            Position position = posRepo.find(trade.uid(), trade.symbol()).orElseGet(() ->
                    Position.builder()
                            .uid(trade.uid())
                            .symbol(trade.symbol())
                            .mode(relatedOrder == null ? MarginMode.CROSS : relatedOrder.getMarginMode())
                            .leverage(BigDecimal.valueOf(relatedOrder == null ? 20 : relatedOrder.getLeverage()))
                            .build()
            );

            if (relatedOrder != null) {
                position.setMode(relatedOrder.getMarginMode());
                position.setLeverage(BigDecimal.valueOf(relatedOrder.getLeverage()));
            }

            BigDecimal oldMargin = safe(position.getMargin());
            PositionChange change = position.applyTradeWithPnl(trade.qty(), trade.price());
            BigDecimal newMargin = requiredPositionMargin(position);
            BigDecimal marginDelta = newMargin.subtract(oldMargin);
            String refId = withSeq.matchId() == null ? String.valueOf(withSeq.seq()) : withSeq.matchId();

            if (marginDelta.signum() > 0) {
                BigDecimal consumed = walletLedgerService.increasePositionMargin(
                        trade.uid(),
                        config.getQuoteAsset(),
                        marginDelta,
                        refId
                );
                consumeOrderReserve(relatedOrder, consumed);
            } else if (marginDelta.signum() < 0) {
                walletLedgerService.releasePositionMargin(
                        trade.uid(),
                        config.getQuoteAsset(),
                        marginDelta.abs(),
                        refId
                );
            }
            position.setMargin(newMargin);

            if (change.hasRealizedPnl()) {
                walletLedgerService.applyRealizedPnl(
                        trade.uid(),
                        config.getQuoteAsset(),
                        change.realizedPnl(),
                        refId
                );
            }

            FeeCalculation fee = feeService.calculate(withSeq, config);
            BigDecimal feeConsumed = walletLedgerService.collectFee(
                    trade.uid(),
                    config.getQuoteAsset(),
                    fee.fee(),
                    refId
            );
            consumeOrderReserve(relatedOrder, feeConsumed);
            if (fee.rebate().signum() > 0) {
                walletLedgerService.creditRebate(
                        trade.uid(),
                        config.getQuoteAsset(),
                        fee.rebate(),
                        refId
                );
            }
            position.addFeePaid(fee.fee());
            position.addRebateEarned(fee.rebate());
            posRepo.save(position);

            // 將帶 seq 的成交事件發布到外部系統（如 Kafka）
            publisher.publish(withSeq);
        }

        for (Order affectedOrder : affectedOrders) {
            reconcileOrderReserve(affectedOrder);
        }

        // 3) 回寫所有受影響訂單
        // -------------------------------------------------
        // 修正點：
        // - 不能只 save 新單本身
        // - 對手單若狀態已變更（PARTIALLY_FILLED / FILLED），也必須保存
        for (Order affectedOrder : affectedOrders) {
            orderRepo.save(affectedOrder);
            publisher.publish(OrderLifecycleEvent.updated(affectedOrder));
        }

        OrderBookSnapshot snapshot = matchingEngine.snapshot(order.getSymbol().code(), 50);
        marketDataService.onTrades(
                order.getSymbol().code(),
                trades,
                snapshot,
                matchingEngine.top(order.getSymbol().code())
        );

        // 資金費與強平由 FundingRateService / LiquidationService 根據標記價獨立觸發。
    }

    private void reconcileOrderReserve(Order order) {
        if (order == null || order.getReservedAmount() == null) return;
        SymbolConfig config = symbolConfigRepository.findBySymbol(order.getSymbol().code())
                .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + order.getSymbol().code()));
        BigDecimal target = order.getPrice() == null
                ? BigDecimal.ZERO
                : riskService.requiredOrderReserve(order, config, order.getPrice());
        BigDecimal diff = order.getReservedAmount().subtract(target);
        if (diff.signum() > 0) {
            walletLedgerService.releaseOrderReserve(
                    order.getUid(),
                    config.getQuoteAsset(),
                    diff,
                    order.getId().toString()
            );
        } else if (diff.signum() < 0) {
            walletLedgerService.reserveOrder(
                    order.getUid(),
                    config.getQuoteAsset(),
                    diff.abs(),
                    order.getId().toString()
            );
        }
        order.setReservedAmount(target);
    }

    private static BigDecimal requiredPositionMargin(Position position) {
        if (position == null || position.getQty() == null || position.getQty().signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal leverage = position.getLeverage() == null || position.getLeverage().signum() <= 0
                ? BigDecimal.ONE
                : position.getLeverage();
        return position.getEntryPrice()
                .multiply(position.getQty().abs())
                .divide(leverage, 18, RoundingMode.HALF_UP);
    }

    private static void consumeOrderReserve(Order order, BigDecimal amount) {
        if (order == null || amount == null || amount.signum() <= 0 || order.getReservedAmount() == null) {
            return;
        }
        BigDecimal remaining = order.getReservedAmount().subtract(amount);
        order.setReservedAmount(remaining.signum() < 0 ? BigDecimal.ZERO : remaining);
    }

    private static BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
