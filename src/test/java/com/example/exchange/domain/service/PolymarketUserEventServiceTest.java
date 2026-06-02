/*
 * 檔案用途：測試 Polymarket user-channel callback idempotency。
 */
package com.example.exchange.domain.service;

import com.example.exchange.domain.model.dto.PolymarketUserWsEvent;
import com.example.exchange.domain.model.entity.PredictionPolymarketOrder;
import com.example.exchange.domain.model.entity.PredictionPolymarketWsEvent;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketOrderRepository;
import com.example.exchange.domain.repository.jpa.PredictionPolymarketWsEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PolymarketUserEventServiceTest {

    @Test
    @DisplayName("相同 user-channel event 重送只保存一次並只更新一次 local order")
    /**
     * 流程：同一 CLOB order/trade event 被 websocket 或 Kafka 重送兩次。
     * 期望：第二次因 eventKey 已存在而直接 no-op，不重複套用 order side effect。
     */
    void duplicateEventReplayDoesNotApplyOrderSideEffectAgain() {
        CountingOrderRepository orderRepository =
                new CountingOrderRepository(order("clob-1"));
        CountingEventRepository eventRepository =
                new CountingEventRepository(false);
        PolymarketUserEventService service =
                new PolymarketUserEventService(
                        new ObjectMapper(),
                        orderRepository.proxy(),
                        eventRepository.proxy(),
                        new PolymarketOrderStateMachine()
                );

        service.handle(event("clob-1", "trade-1"));
        service.handle(event("clob-1", "trade-1"));

        assertThat(eventRepository.saveCount)
                .isEqualTo(1);
        assertThat(orderRepository.saveCount)
                .isEqualTo(1);
        assertThat(orderRepository.order.getTradeStatus())
                .isEqualTo("MATCHED");
        assertThat(orderRepository.order.getStatus())
                .isEqualTo("ORDER_STATUS_MATCHED");
        assertThat(orderRepository.order.getLastTradeId())
                .isEqualTo("trade-1");
    }

    @Test
    @DisplayName("eventKey 唯一鍵 race 會被當成 duplicate replay，避免 consumer 重試")
    /**
     * 流程：兩個 consumer 併發處理同一 callback，第二個在 find 後 save 前才撞到唯一鍵。
     * 期望：duplicate-key 被吞成 replay no-op，也不查找或更新 local order。
     */
    void duplicateKeyRaceReturnsWithoutOrderSideEffect() {
        CountingOrderRepository orderRepository =
                new CountingOrderRepository(order("clob-1"));
        CountingEventRepository eventRepository =
                new CountingEventRepository(true);
        PolymarketUserEventService service =
                new PolymarketUserEventService(
                        new ObjectMapper(),
                        orderRepository.proxy(),
                        eventRepository.proxy(),
                        new PolymarketOrderStateMachine()
                );

        service.handle(event("clob-1", "trade-1"));

        assertThat(eventRepository.saveCount)
                .isEqualTo(1);
        assertThat(orderRepository.findCount)
                .isZero();
        assertThat(orderRepository.saveCount)
                .isZero();
    }

    @Test
    @DisplayName("payload-only trade event 會持久化到 local Polymarket order lifecycle projection")
    /**
     * 流程：Polymarket user event top-level 沒有 orderId/tradeId，但 payload 帶 order_id/trade_id。
     * 期望：event row 保存 resolved ids，local order projection 更新 matched lifecycle 與 lastTradeId。
     */
    void payloadOnlyTradeEventUpdatesLocalOrderLifecycleProjection() {
        CountingOrderRepository orderRepository =
                new CountingOrderRepository(order("clob-2"));
        CountingEventRepository eventRepository =
                new CountingEventRepository(false);
        PolymarketUserEventService service =
                new PolymarketUserEventService(
                        new ObjectMapper(),
                        orderRepository.proxy(),
                        eventRepository.proxy(),
                        new PolymarketOrderStateMachine()
                );

        service.handle(payloadOnlyTradeEvent("clob-2", "trade-2"));

        assertThat(eventRepository.saved.getOrderId())
                .isEqualTo("clob-2");
        assertThat(eventRepository.saved.getTradeId())
                .isEqualTo("trade-2");
        assertThat(orderRepository.findCount)
                .isEqualTo(1);
        assertThat(orderRepository.saveCount)
                .isEqualTo(1);
        assertThat(orderRepository.order.getStatus())
                .isEqualTo("ORDER_STATUS_MATCHED");
        assertThat(orderRepository.order.getTradeStatus())
                .isEqualTo("MATCHED");
        assertThat(orderRepository.order.getLastTradeId())
                .isEqualTo("trade-2");
    }

    private static PolymarketUserWsEvent event(
            String orderId,
            String tradeId
    ) {
        return PolymarketUserWsEvent.builder()
                .eventType("trade")
                .status("MATCHED")
                .walletAddress("0x0000000000000000000000000000000000000001")
                .market("condition-1")
                .assetId("asset-1")
                .orderId(orderId)
                .tradeId(tradeId)
                .payload(Map.of("order_id", orderId, "trade_id", tradeId))
                .build();
    }

    private static PolymarketUserWsEvent payloadOnlyTradeEvent(
            String orderId,
            String tradeId
    ) {
        return PolymarketUserWsEvent.builder()
                .eventType("trade")
                .status("MATCHED")
                .walletAddress("0x0000000000000000000000000000000000000001")
                .market("condition-2")
                .assetId("asset-2")
                .payload(Map.of("order_id", orderId, "trade_id", tradeId))
                .build();
    }

    private static PredictionPolymarketOrder order(String clobOrderId) {
        PredictionPolymarketOrder order =
                new PredictionPolymarketOrder();
        order.setInternalOrderId("internal-1");
        order.setClobOrderId(clobOrderId);
        order.setUserId("user-1");
        order.setSessionId("session-1");
        order.setMarketSlug("market-1");
        order.setStatus("ACCEPTED");
        return order;
    }

    private static class CountingOrderRepository {
        private final PredictionPolymarketOrder order;
        private int findCount;
        private int saveCount;

        private CountingOrderRepository(PredictionPolymarketOrder order) {
            this.order = order;
        }

        private PredictionPolymarketOrderRepository proxy() {
            return (PredictionPolymarketOrderRepository) Proxy.newProxyInstance(
                    PredictionPolymarketOrderRepository.class.getClassLoader(),
                    new Class<?>[]{PredictionPolymarketOrderRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByClobOrderId" -> {
                            findCount++;
                            yield order.getClobOrderId().equals(args[0])
                                    ? Optional.of(order)
                                    : Optional.empty();
                        }
                        case "save" -> {
                            saveCount++;
                            yield args[0];
                        }
                        case "toString" -> "CountingOrderRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static class CountingEventRepository {
        private final boolean throwDuplicateOnSave;
        private PredictionPolymarketWsEvent saved;
        private int saveCount;

        private CountingEventRepository(boolean throwDuplicateOnSave) {
            this.throwDuplicateOnSave = throwDuplicateOnSave;
        }

        private PredictionPolymarketWsEventRepository proxy() {
            return (PredictionPolymarketWsEventRepository) Proxy.newProxyInstance(
                    PredictionPolymarketWsEventRepository.class.getClassLoader(),
                    new Class<?>[]{PredictionPolymarketWsEventRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByEventKey" -> saved != null
                                && saved.getEventKey().equals(args[0])
                                ? Optional.of(saved)
                                : Optional.empty();
                        case "save" -> {
                            saveCount++;
                            if (throwDuplicateOnSave) {
                                throw new DataIntegrityViolationException("duplicate event_key");
                            }
                            saved = (PredictionPolymarketWsEvent) args[0];
                            yield saved;
                        }
                        case "toString" -> "CountingEventRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
