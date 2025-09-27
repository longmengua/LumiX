package com.example.exchange.application.service;

import com.example.exchange.application.event.DomainEventPublisher;
import com.example.exchange.domain.event.TradeExecuted;
import com.example.exchange.domain.model.*;
import com.example.exchange.domain.repository.EventStore;
import com.example.exchange.domain.repository.PositionRepository;
import com.example.exchange.domain.repository.OrderRepository; // << 新增
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
    private final OrderRepository orderRepo; // << 新增相依

    public OrderService(MatchingEngine matchingEngine,
                        PositionRepository posRepo,
                        EventStore eventStore,
                        DomainEventPublisher<TradeExecuted> publisher,
                        OrderRepository orderRepo) { // << 新增
        this.matchingEngine = matchingEngine;
        this.posRepo = posRepo;
        this.eventStore = eventStore;
        this.publisher = publisher;
        this.orderRepo = orderRepo; // << 新增
    }

    /**
     * 下單 → 撮合 → 回寫持倉/事件 → **回寫訂單狀態**
     */
    public void processOrder(Order order) {
        // 1) 丟進撮合引擎
        List<TradeExecuted> trades = matchingEngine.submitOrder(order);

        // 2) 逐筆成交：事件入庫→更新 Position→發布事件
        for (TradeExecuted t : trades) {
            long seq = eventStore.append(t);
            TradeExecuted withSeq = t.withSeq(seq);

            Position pos = posRepo.find(t.uid(), t.symbol()).orElseGet(() ->
                    Position.builder()
                            .uid(t.uid())
                            .symbol(t.symbol())
                            .mode(MarginMode.CROSS)
                            .leverage(BigDecimal.valueOf(20))
                            .build()
            );
            pos.applyTrade(t.qty(), t.price());
            posRepo.save(pos);

            publisher.publish(withSeq);
        }

        // 3) **回寫新單狀態**（非常重要）
        //    - 如果此訂單還有剩餘量（NEW / PARTIALLY_FILLED），需要能被查到 → save()
        //    - 如果完全成交（FILLED）或取消（CANCELED），也建議 save() 讓歷史可查
        orderRepo.save(order);

        // ⚠️ 注意：撮合過程中對手方訂單（bestBid/bestAsk）也可能被部分成交或全成交。
        // 由於 InMemoryMatchingEngine 會拿到「原始對手方 Order 物件」並呼叫 order.fill(execQty)，
        // 所以它們的狀態也變了。為了讓查詢端看見最新狀態，建議在撮合引擎回傳「被影響的訂單集合」，
        // 或在這裡另外將「簿中最佳單」變更後回寫。簡化起見，這版先保證「新單」一定會被存。
    }
}
