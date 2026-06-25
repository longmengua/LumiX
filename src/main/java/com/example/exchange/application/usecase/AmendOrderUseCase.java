/*
 * 檔案用途：UseCase 入口，承接介面層請求並協調應用服務執行業務動作。
 */
package com.example.exchange.application.usecase;

import com.example.exchange.application.command.AmendOrderCommand;
import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.application.service.CommandTransactionBoundary;
import com.example.exchange.application.service.MarketDataService;
import com.example.exchange.application.service.MatchingWorkerExecutionService;
import com.example.exchange.application.service.MatchingWorkerLifecycleService;
import com.example.exchange.application.service.RiskService;
import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.dto.TopOfBook;
import com.example.exchange.domain.model.dto.Order;
import com.example.exchange.domain.model.dto.SymbolConfig;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.model.enums.OrderType;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.SymbolConfigRepository;
import com.example.exchange.domain.service.MatchingEngine;
import com.example.exchange.domain.service.OrderBookSnapshot;
import com.example.exchange.interfaces.web.dto.OrderInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AmendOrderUseCase {

    private final OrderRepository orderRepository;
    private final SymbolConfigRepository symbolConfigRepository;
    private final MatchingEngine matchingEngine;
    private final RiskService riskService;
    private final MarketDataService marketDataService;
    private final DomainEventPublisher<Object> publisher;
    private CommandTransactionBoundary commandTransactionBoundary;
    private MatchingWorkerLifecycleService matchingWorkerLifecycleService;
    private MatchingWorkerExecutionService matchingWorkerExecutionService;

    @Autowired(required = false)
    public void setCommandTransactionBoundary(CommandTransactionBoundary commandTransactionBoundary) {
        this.commandTransactionBoundary = commandTransactionBoundary;
    }

    @Autowired(required = false)
    public void setMatchingWorkerLifecycleService(MatchingWorkerLifecycleService matchingWorkerLifecycleService) {
        this.matchingWorkerLifecycleService = matchingWorkerLifecycleService;
    }

    @Autowired(required = false)
    public void setMatchingWorkerExecutionService(MatchingWorkerExecutionService matchingWorkerExecutionService) {
        this.matchingWorkerExecutionService = matchingWorkerExecutionService;
    }

    /**
     * 修改仍在簿內的 LIMIT 掛單。
     *
     * <p>amend 保持 maker-only 語意：若新價格會立即吃單，要求呼叫端改用 cancel-replace。
     * 成功後會重新計算剩餘委託的 reserve，並推送 order lifecycle / depth delta。</p>
     */
    public OrderInfoResponse handle(AmendOrderCommand command) {
        if (commandTransactionBoundary != null) {
            return commandTransactionBoundary.execute("amend-order", () -> handleInsideTransaction(command));
        }
        return handleInsideTransaction(command);
    }

    private OrderInfoResponse handleInsideTransaction(AmendOrderCommand command) {
        validateCommand(command);

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + command.orderId()));
        if (order.getUid() != command.uid()) {
            throw new IllegalArgumentException("order uid mismatch");
        }
        if (!isOpen(order)) {
            throw new IllegalStateException("only open orders can be amended");
        }
        if (order.getType() != OrderType.LIMIT) {
            throw new IllegalArgumentException("only LIMIT orders can be amended");
        }

        BigDecimal newPrice = command.price() == null ? order.getPrice() : command.price();
        BigDecimal newQty = command.qty() == null ? order.getQty() : command.qty();
        String newClientOrderId = command.clientOrderId() == null
                ? order.getClientOrderId()
                : command.clientOrderId();

        SymbolConfig config = symbolConfigRepository.findBySymbol(order.getSymbol().code())
                .orElseThrow(() -> new IllegalArgumentException("missing symbol config: " + order.getSymbol().code()));
        // 先用候選訂單跑風控，避免真正修改 book 後才發現 reserve 或交易規則不合法。
        Order candidate = amendedCandidate(order, newPrice, newQty, newClientOrderId);
        if (wouldTakeLiquidity(candidate)) {
            throw new IllegalArgumentException("amend would take liquidity; use cancel-replace instead");
        }

        BigDecimal targetReserve = riskService.validateAmend(candidate, config);
        boolean amended = amendThroughConfiguredMatchingPath(order, newPrice, newQty);
        if (!amended) {
            throw new IllegalStateException("open order not found in matching book");
        }

        order.setOrigQty(order.getExecutedQty().add(newQty));
        order.setClientOrderId(newClientOrderId);
        order.setStatus(order.getExecutedQty().signum() > 0 ? Order.Status.PARTIALLY_FILLED : Order.Status.NEW);
        riskService.reconcileOrderReserve(order, config, targetReserve);
        orderRepository.save(order);
        publisher.publish(OrderLifecycleEvent.updated(order));

        OrderBookSnapshot snapshot = matchingEngine.snapshot(order.getSymbol().code(), 50);
        marketDataService.onOrderBookChanged(
                order.getSymbol().code(),
                snapshot,
                matchingEngine.top(order.getSymbol().code())
        );
        return order.toOrderInfoResponse();
    }

    /** 驗證 amend 至少有一個可變欄位，並擋掉非正數價格或數量。 */
    private void validateCommand(AmendOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("amend order command cannot be null");
        }
        if (command.orderId() == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        if (command.price() == null && command.qty() == null && command.clientOrderId() == null) {
            throw new IllegalArgumentException("price, qty or clientOrderId is required");
        }
        if (command.price() != null && command.price().signum() <= 0) {
            throw new IllegalArgumentException("price must be greater than zero");
        }
        if (command.qty() != null && command.qty().signum() <= 0) {
            throw new IllegalArgumentException("qty must be greater than zero");
        }
    }

    /** 判斷新價格是否會穿過對手方 best price；amend 不允許主動吃單。 */
    private boolean wouldTakeLiquidity(Order candidate) {
        Optional<TopOfBook> top = matchingEngine.top(candidate.getSymbol().code());
        if (top.isEmpty()) return false;
        BigDecimal opposite = candidate.getSide() == OrderSide.BUY
                ? top.get().getBestAsk()
                : top.get().getBestBid();
        if (opposite == null || opposite.signum() <= 0) return false;
        return candidate.getSide() == OrderSide.BUY
                ? candidate.getPrice().compareTo(opposite) >= 0
                : candidate.getPrice().compareTo(opposite) <= 0;
    }

    /** 建立不入庫的候選訂單，供風控與 reserve 計算使用。 */
    private static Order amendedCandidate(
            Order order,
            BigDecimal newPrice,
            BigDecimal newQty,
            String newClientOrderId
    ) {
        return Order.builder()
                .id(order.getId())
                .uid(order.getUid())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .type(order.getType())
                .price(newPrice)
                .qty(newQty)
                .origQty(order.getExecutedQty().add(newQty))
                .executedQty(order.getExecutedQty())
                .avgPrice(order.getAvgPrice())
                .timeInForce(order.getTimeInForce())
                .reduceOnly(order.isReduceOnly())
                .postOnly(order.isPostOnly())
                .leverage(order.getLeverage())
                .marginMode(order.getMarginMode())
                .reservedAmount(order.getReservedAmount())
                .reservedAsset(order.getReservedAsset())
                .makerFeeRateSnapshot(order.getMakerFeeRateSnapshot())
                .takerFeeRateSnapshot(order.getTakerFeeRateSnapshot())
                .clientOrderId(newClientOrderId)
                .strategyId(order.getStrategyId())
                .marketMakerId(order.getMarketMakerId())
                .status(order.getExecutedQty().signum() > 0 ? Order.Status.PARTIALLY_FILLED : Order.Status.NEW)
                .ctime(order.getCtime())
                .build();
    }

    private static boolean isOpen(Order order) {
        return order.getStatus() == Order.Status.NEW
                || order.getStatus() == Order.Status.PARTIALLY_FILLED;
    }

    private boolean amendThroughConfiguredMatchingPath(Order order, BigDecimal newPrice, BigDecimal newQty) {
        Optional<MatchingWorkerLifecycleService.MatchingWorkerOwnerContext> context = workerContext(order);
        if (context.isEmpty()) {
            return matchingEngine.amendOrder(order, newPrice, newQty);
        }
        MatchingWorkerLifecycleService.MatchingWorkerOwnerContext owner = context.get();
        MatchingResult result = matchingWorkerExecutionService.amend(
                order,
                newPrice,
                newQty,
                owner.ownerId(),
                owner.ownerEpoch()
        );
        return result.getAffectedOrders() != null && !result.getAffectedOrders().isEmpty();
    }

    private Optional<MatchingWorkerLifecycleService.MatchingWorkerOwnerContext> workerContext(Order order) {
        if (matchingWorkerLifecycleService == null || matchingWorkerExecutionService == null
                || order == null || order.getSymbol() == null) {
            return Optional.empty();
        }
        return matchingWorkerLifecycleService.routingOwnerContext(order.getSymbol().code());
    }
}
