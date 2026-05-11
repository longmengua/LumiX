package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.enums.MarginMode;
import com.example.exchange.domain.model.dto.MatchingResult;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.Position;
import com.example.exchange.domain.repository.EventStore;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.service.MatchingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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
 * - 目前屬於簡化版流程，先把撮合 → 持倉 → 事件 → 訂單回寫這條主線打通
 *
 * 修正重點：
 * 1) 不再使用舊版 submitOrder()，統一改走 matchingEngine.submit(order)
 * 2) 使用 MatchingResult 取得：
 *    - trades：成交事件列表
 *    - affectedOrders：所有受影響訂單
 * 3) 回寫所有受影響訂單，而不只是新單本身
 *
 * 後續可擴充：
 * - 下單前風控（IM / MM / reduceOnly / 可用餘額）
 * - 手續費計算與凍結/解凍
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
    private final DomainEventPublisher<TradeExecuted> publisher;

    /**
     * 訂單儲存庫
     * - 用於保存訂單狀態，供查詢掛單 / 歷史單
     */
    private final OrderRepository orderRepo;

    // TODO: 注入資金/費率/帳戶資產服務
    // private final AccountService accountSvc;         // 查詢&操作餘額、凍結/解凍
    // private final FeeService feeSvc;                 // 計算 maker/taker 手續費、VIP/券折抵
    // private final FundingService fundingSvc;         // 資金費（永續合約），計提與結算
    // private final RiskService riskSvc;               // 保證金比率、強平監控
    // private final InsuranceFundService insuranceSvc; // 保險基金（強平虧損兜底）

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
        // TODO: 根據 order.side / order.type / timeInForce 推導更完整的下單上下文
        // TODO: 這裡之後可加入 reduceOnly 驗證、可用餘額檢查、初始保證金檢查等

        // 1) 送入撮合引擎，取得撮合結果
        MatchingResult result = matchingEngine.submit(order);

        // 本次撮合產生的成交事件
        List<TradeExecuted> trades = result.getTrades();

        // 本次撮合中所有受影響的訂單（新單 + 對手單）
        List<Order> affectedOrders = result.getAffectedOrders();

        // 2) 逐筆成交：事件入庫 → 更新倉位 → 發布事件
        for (TradeExecuted trade : trades) {
            // 將成交事件寫入 EventStore，取得單調序號
            long seq = eventStore.append(trade);

            // 回填 seq，作為往外發布或後續 replay 用事件
            TradeExecuted withSeq = trade.withSeq(seq);

            // === TODO: 計費與保證金（以每筆成交為單位） ===
            // TODO: 判定此筆成交對於當事人是 maker 還是 taker（引擎可擴充 role）
            // TODO: 計算手續費 fee = notional * feeRate（支援 VIP、持倉等級、券/點卡抵扣）
            // TODO: 計提資金費（若為永續合約）：
            //       - 將 notional / 方向 累加到 funding accrual bucket
            // TODO: 計算/更新逐筆實現損益（partial close 時）
            // TODO: 以成交價更新保證金需求（IM / MM），並調整已凍結資金

            // 查詢或建立當前持倉（簡化：預設 CROSS + 20x）
            Position position = posRepo.find(trade.uid(), trade.symbol()).orElseGet(() ->
                    Position.builder()
                            .uid(trade.uid())
                            .symbol(trade.symbol())
                            .mode(MarginMode.CROSS)
                            .leverage(BigDecimal.valueOf(20))
                            .build()
            );

            // 套用成交，更新持倉數量與均價
            // TODO: Position.applyTrade 之後可擴充：
            // - 已實現盈虧（RPnL）
            // - 未實現盈虧（UPnL）
            // - 維持保證金需求
            position.applyTrade(trade.qty(), trade.price());
            posRepo.save(position);

            // TODO: 扣除手續費（可從可用餘額或保證金中扣減）
            // TODO: 若首次成交，可重算預凍費率上限並部分釋放
            // TODO: 成交後重新評估風險，必要時觸發 liquidation 流程

            // 將帶 seq 的成交事件發布到外部系統（如 Kafka）
            publisher.publish(withSeq);
        }

        // 3) 回寫所有受影響訂單
        // -------------------------------------------------
        // 修正點：
        // - 不能只 save 新單本身
        // - 對手單若狀態已變更（PARTIALLY_FILLED / FILLED），也必須保存
        for (Order affectedOrder : affectedOrders) {
            orderRepo.save(affectedOrder);
        }

        // 4) 凍結資金調整 / 釋放（出入點）
        // -------------------------------------------------
        // TODO: 若訂單完全成交 → 釋放剩餘凍結（多退少補）
        // TODO: 若訂單部分成交 → 按剩餘名義價值重算凍結
        // TODO: 若 MARKET 殘量轉 LIMIT 掛簿（MTL）→ 依新掛簿價重估 IM / fee 上限

        // 5) 資金費計提與結算（出入點）
        // -------------------------------------------------
        // TODO: 若到資金費結算瞬間（fundingSvc.shouldSettle(symbol, now)）：
        //       - 根據標記價格 / 指數價計算 funding
        //       - 對多空雙邊倉位做轉移（多 → 空 或 空 → 多）
        //       - 若資金不足，從可用 / 保證金扣減；不足則進一步風控處理
        //       - 記錄 funding 流水與事件（FundingSettled）

        // 6) 保險基金 / 強平清算（出入點）
        // -------------------------------------------------
        // TODO: 若強平導致負資產 → 由保險基金 / ADL 處理
        // TODO: 記錄保險基金入出帳流水，確保事件可回放、可審計
    }
}