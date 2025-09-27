package com.example.java21_OLAP.application.service;

import com.example.java21_OLAP.application.event.DomainEventPublisher;
import com.example.java21_OLAP.domain.event.TradeExecuted;
import com.example.java21_OLAP.domain.model.*;
import com.example.java21_OLAP.domain.repository.EventStore;
import com.example.java21_OLAP.domain.repository.PositionRepository;
import com.example.java21_OLAP.domain.service.MatchingEngine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 訂單/成交服務
 *
 * - 現在不再「假裝成交」，而是把訂單交給 MatchingEngine
 * - 撮合回來的每一筆 TradeExecuted：
 *     1) append 到 EventStore（取得 seq）
 *     2) 作用到對應 uid 的 Position（買 +qty；賣 -qty）
 *     3) 發布事件（Kafka/其他），供外部副作用（快取、審計、推播）
 */
@Service
public class OrderService {

    private final MatchingEngine matchingEngine;
    private final PositionRepository posRepo;
    private final EventStore eventStore;
    private final DomainEventPublisher<TradeExecuted> publisher;

    public OrderService(MatchingEngine matchingEngine,
                        PositionRepository posRepo,
                        EventStore eventStore,
                        DomainEventPublisher<TradeExecuted> publisher) {
        this.matchingEngine = matchingEngine;
        this.posRepo = posRepo;
        this.eventStore = eventStore;
        this.publisher = publisher;
    }

    /**
     * 下單 → 撮合 → 回寫持倉與事件
     * @param order 建立好的訂單（LIMIT 或 MARKET 模擬成極端價格）
     */
    public void processOrder(Order order) {
        // 1) 丟進撮合引擎
        List<TradeExecuted> trades = matchingEngine.submitOrder(order);

        // 2) 處理每筆成交
        for (TradeExecuted t : trades) {
            // 2.1 事件入庫並取得 seq
            long seq = eventStore.append(t);
            TradeExecuted withSeq = t.withSeq(seq);

            // 2.2 作用到 Position（注意 qty 帶方向：買 +，賣 -）
            Position pos = posRepo.find(t.uid(), t.symbol())
                    .orElseGet(() -> new Position(t.uid(), t.symbol(), MarginMode.CROSS, BigDecimal.valueOf(20)));
            pos.applyTrade(t.qty(), t.price());
            posRepo.save(pos);

            // 2.3 對外發布（Kafka 等）
            publisher.publish(withSeq);
        }
    }
}
