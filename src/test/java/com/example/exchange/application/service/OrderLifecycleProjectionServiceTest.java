/*
 * 檔案用途：測試 order lifecycle durable event log 與 projection replay 行為。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.model.entity.Order;
import com.example.exchange.domain.model.entity.OrderLifecycleEventRecord;
import com.example.exchange.domain.model.entity.OrderLifecycleProjection;
import com.example.exchange.domain.model.entity.Symbol;
import com.example.exchange.domain.repository.OrderLifecycleEventStore;
import com.example.exchange.domain.repository.OrderLifecycleProjectionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLifecycleProjectionServiceTest {

    @Test
    @DisplayName("record 會 append durable event 並更新最新 order lifecycle projection")
    /**
     * 流程：寫入 CREATED 與 FILLED lifecycle event -> 驗證 event log 保留兩筆，projection 更新為最新成交狀態。
     */
    void recordAppendsEventAndUpdatesProjection() {
        MemEventStore eventStore = new MemEventStore();
        MemProjectionStore projectionStore = new MemProjectionStore();
        OrderLifecycleProjectionService service = new OrderLifecycleProjectionService(eventStore, projectionStore);
        UUID orderId = UUID.randomUUID();

        service.record(event(orderId, OrderLifecycleEvent.Stage.CREATED, Order.Status.NEW, "0", "1.000", "0"));
        service.record(event(orderId, OrderLifecycleEvent.Stage.FILLED, Order.Status.FILLED, "1.000", "0", "100.00"));

        List<OrderLifecycleEventRecord> history = service.history(orderId);
        OrderLifecycleProjection projection = service.projection(orderId).orElseThrow();

        assertThat(history).hasSize(2);
        assertThat(history).extracting(OrderLifecycleEventRecord::getSchemaVersion)
                .containsOnly(OrderLifecycleProjectionService.SCHEMA_VERSION);
        assertThat(projection.getOrderId()).isEqualTo(orderId.toString());
        assertThat(projection.getStrategyId()).isEqualTo("strategy-42");
        assertThat(projection.getMarketMakerId()).isEqualTo("mm-42");
        assertThat(projection.getLatestStage()).isEqualTo("FILLED");
        assertThat(projection.getStatus()).isEqualTo("FILLED");
        assertThat(projection.getExecutedQty()).isEqualByComparingTo("1.000");
        assertThat(projection.getRemainingQty()).isEqualByComparingTo("0");
        assertThat(projectionStore.findByUidAndSymbol(42, "BTCUSDT")).containsExactly(projection);
    }

    @Test
    @DisplayName("rebuildProjection 會用 event log replay 重建 projection")
    /**
     * 流程：先寫入兩筆 lifecycle event -> 清空 projection store -> replay 單張訂單 -> 驗證 projection 可由 event log 重建。
     */
    void rebuildProjectionReplaysEventLog() {
        MemEventStore eventStore = new MemEventStore();
        MemProjectionStore projectionStore = new MemProjectionStore();
        OrderLifecycleProjectionService service = new OrderLifecycleProjectionService(eventStore, projectionStore);
        UUID orderId = UUID.randomUUID();

        service.record(event(orderId, OrderLifecycleEvent.Stage.ACCEPTED, Order.Status.NEW, "0", "1.000", "0"));
        service.record(event(orderId, OrderLifecycleEvent.Stage.CANCELED, Order.Status.CANCELED, "0", "1.000", "0"));
        projectionStore.clear();

        Optional<OrderLifecycleProjection> rebuilt = service.rebuildProjection(orderId);

        assertThat(rebuilt).isPresent();
        assertThat(rebuilt.orElseThrow().getLatestStage()).isEqualTo("CANCELED");
        assertThat(service.projection(orderId)).isPresent();
    }

    private static OrderLifecycleEvent event(
            UUID orderId,
            OrderLifecycleEvent.Stage stage,
            Order.Status status,
            String executedQty,
            String remainingQty,
            String avgPrice
    ) {
        return new OrderLifecycleEvent(
                orderId,
                42,
                Symbol.builder().base("BTC").quote("USDT").priceScale(2).qtyScale(3).build(),
                "client-42",
                "strategy-42",
                "mm-42",
                stage,
                status,
                status == Order.Status.CANCELED ? "USER_REQUEST" : null,
                new BigDecimal("100.00"),
                new BigDecimal("1.000"),
                new BigDecimal(remainingQty),
                new BigDecimal(executedQty),
                new BigDecimal(avgPrice),
                Instant.now()
        );
    }

    private static class MemEventStore implements OrderLifecycleEventStore {
        private final AtomicLong ids = new AtomicLong();
        private final List<OrderLifecycleEventRecord> records = new ArrayList<>();

        @Override
        public void append(OrderLifecycleEventRecord record) {
            record.setId(ids.incrementAndGet());
            records.add(record);
        }

        @Override
        public List<OrderLifecycleEventRecord> findByOrderId(String orderId) {
            return records.stream()
                    .filter(record -> record.getOrderId().equals(orderId))
                    .sorted(Comparator
                            .comparing(OrderLifecycleEventRecord::getEventTs)
                            .thenComparing(OrderLifecycleEventRecord::getId))
                    .toList();
        }
    }

    private static class MemProjectionStore implements OrderLifecycleProjectionStore {
        private final Map<String, OrderLifecycleProjection> projections = new LinkedHashMap<>();

        @Override
        public void save(OrderLifecycleProjection projection) {
            projections.put(projection.getOrderId(), projection);
        }

        @Override
        public Optional<OrderLifecycleProjection> findByOrderId(String orderId) {
            return Optional.ofNullable(projections.get(orderId));
        }

        @Override
        public List<OrderLifecycleProjection> findByUid(long uid) {
            return projections.values().stream()
                    .filter(projection -> projection.getUid() == uid)
                    .toList();
        }

        @Override
        public List<OrderLifecycleProjection> findByUidAndSymbol(long uid, String symbol) {
            return projections.values().stream()
                    .filter(projection -> projection.getUid() == uid)
                    .filter(projection -> projection.getSymbol().equals(symbol))
                    .toList();
        }

        @Override
        public Optional<OrderLifecycleProjection> findByClientOrderId(String clientOrderId) {
            return projections.values().stream()
                    .filter(projection -> clientOrderId.equals(projection.getClientOrderId()))
                    .findFirst();
        }

        void clear() {
            projections.clear();
        }
    }
}
