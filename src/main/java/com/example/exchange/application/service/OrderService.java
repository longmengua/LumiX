package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.*;
import com.example.exchange.domain.repository.EventStore;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.OrderRepository;
import com.example.exchange.domain.service.MatchingEngine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 訂單/成交服務
 *
 * 修正重點：
 * - 下單後，**必須把訂單狀態寫入 OrderRepository**（例如 Redis），
 *   讓 /api/order/open 能查到。
 * - 每筆成交後，也要把雙方訂單的剩餘量/狀態回寫（若有持續掛單）。
 */
@Service
public class OrderService {

    private final MatchingEngine matchingEngine;
    private final PositionRepository posRepo;
    private final EventStore eventStore;
    private final DomainEventPublisher<TradeExecuted> publisher;
    private final OrderRepository orderRepo;

    // TODO: 注入資金/費率/帳戶資產服務
    // private final AccountService accountSvc;         // 查詢&操作餘額、凍結/解凍
    // private final FeeService feeSvc;                 // 計算 maker/taker 手續費、VIP/券折抵
    // private final FundingService fundingSvc;         // 資金費（永續合約），計提與結算
    // private final RiskService riskSvc;               // 保證金比率、強平監控
    // private final InsuranceFundService insuranceSvc; // 保險基金（強平虧損兜底）

    public OrderService(MatchingEngine matchingEngine,
                        PositionRepository posRepo,
                        EventStore eventStore,
                        DomainEventPublisher<TradeExecuted> publisher,
                        OrderRepository orderRepo) {
        this.matchingEngine = matchingEngine;
        this.posRepo = posRepo;
        this.eventStore = eventStore;
        this.publisher = publisher;
        this.orderRepo = orderRepo;
    }

    /**
     * 下單 → 撮合 → 回寫持倉/事件 → 回寫訂單狀態 →（資金/費用/MtM 等處理）
     */
    public void processOrder(Order order) {
        // TODO: 根據 order.side/type 推導可能的 maker/taker 身份（預估用）

        // 1) 丟進撮合引擎
        List<TradeExecuted> trades = matchingEngine.submitOrder(order);

        // 2) 逐筆成交：事件入庫→更新 Position→發布事件
        for (TradeExecuted t : trades) {
            long seq = eventStore.append(t);
            TradeExecuted withSeq = t.withSeq(seq);

            // === TODO: 計費與保證金（以每筆成交為單位） ===
            // TODO: 判定此筆成交對於當事人是 maker 還是 taker（引擎需回傳角色或由訂單簿狀態判斷）
            // TODO: 計算手續費 fee = notional * feeRate（支援 VIP、持倉等級、券/點卡抵扣）
            // TODO: 計提資金費（若為永續合約）：
            //       - 將 notional/方向 累加到 funding accrual bucket（到資金費結算時間再批量結算）
            // TODO: 計算/更新逐筆實現損益（partial close 時）
            // TODO: 以成交價更新保證金需求（IM/維持保證金 MM），並調整已凍結金額

            Position pos = posRepo.find(t.uid(), t.symbol()).orElseGet(() ->
                    Position.builder()
                            .uid(t.uid())
                            .symbol(t.symbol())
                            .mode(MarginMode.CROSS)
                            .leverage(BigDecimal.valueOf(20))
                            .build()
            );

            // TODO: pos.applyTrade 內部需同時刷新：
            // - 倉位方向/数量/均價
            // - 已實現盈虧（若減倉）
            // - 未實現盈虧（可延後到標記價格變動時計算）
            pos.applyTrade(t.qty(), t.price());
            posRepo.save(pos);

            // TODO: 扣除手續費（資產帳戶可用額減少，或從保證金中扣）
            // TODO: 更新/調整凍結（若為首次成交，可能將部分預凍費率下調）
            // TODO: 強平風控：若成交後保證金率 < 閾值 → 觸發逐級減倉/強平流程入隊

            publisher.publish(withSeq);
        }

        // 3) **回寫新單狀態**
        //    - 若仍有剩餘量（NEW/PARTIALLY_FILLED）→ save() 以便 /api/order/open 查詢
        //    - FILLED/CANCELED 也建議保存史料以供追溯
        orderRepo.save(order);

        // 4) **凍結資金調整/釋放**
        // TODO: 若訂單完全成交 → 釋放剩餘凍結（多退少補）；若部分成交 → 按剩餘名義金額重算凍結
        // TODO: 若 MTL 轉掛且仍在簿 → 按最新掛簿價重估 IM & 手續費上限凍結

        // 5) **資金費計提與結算（出入點）**
        // TODO: 若到資金費結算瞬間（ fundingSvc.shouldSettle(symbol, now) ）：
        //       - 根據標記價格/指數價計算資金費
        //       - 對多空雙邊倉位做轉移（多→空或空→多）
        //       - 考慮資金費不足時從可用/保證金扣減，不足觸發強平保護
        //       - 記錄事件/流水（可發布 FundingSettled 事件）

        // 6) **保險基金/強平清算（出入點）**
        // TODO: 若強平導致負資產 → 由保險基金/自動減倉（ADL）處理
        // TODO: 記錄對保險基金的入出帳流水，確保審計可回放
    }
}
