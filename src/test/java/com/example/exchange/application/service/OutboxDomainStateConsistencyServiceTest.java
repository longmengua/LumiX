/*
 * 檔案用途：測試 outbox row 與 domain-state transition consistency report。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.OutboxDomainStateConsistencyReport;
import com.example.exchange.domain.model.entity.OrderLifecycleProjection;
import com.example.exchange.domain.model.entity.OutboxEvent;
import com.example.exchange.domain.repository.OrderLifecycleProjectionStore;
import com.example.exchange.domain.repository.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxDomainStateConsistencyServiceTest {

    @Test
    @DisplayName("inspectLatest 會回報 order.lifecycle outbox row 缺少對應 projection")
    void inspectLatestReportsOrderLifecycleOutboxWithoutProjection() {
        MemOutboxRepository outboxRepository = new MemOutboxRepository();
        MemOrderLifecycleProjectionStore projectionStore = new MemOrderLifecycleProjectionStore();
        UUID missingOrderId = UUID.randomUUID();
        outboxRepository.save(OutboxEvent.builder()
                .topic("order.lifecycle")
                .eventKey("BTCUSDT:" + missingOrderId)
                .eventType("OrderLifecycleEvent")
                .createdAt(Instant.parse("2026-05-30T00:00:00Z"))
                .build());
        OutboxDomainStateConsistencyService service =
                new OutboxDomainStateConsistencyService(outboxRepository, projectionStore);

        // 場景：outbox row 已落庫，但對應 order lifecycle projection 不存在，表示需要 recovery/repair。
        OutboxDomainStateConsistencyReport report = service.inspectLatest(10);

        assertThat(report.scanned()).isEqualTo(1);
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues().getFirst().issueType()).isEqualTo("MISSING_ORDER_LIFECYCLE_PROJECTION");
        assertThat(report.issues().getFirst().eventKey()).isEqualTo("BTCUSDT:" + missingOrderId);
    }

    @Test
    @DisplayName("inspectLatest 對已有 projection 的 order.lifecycle outbox row 不報 issue")
    void inspectLatestIgnoresOrderLifecycleOutboxWithProjection() {
        MemOutboxRepository outboxRepository = new MemOutboxRepository();
        MemOrderLifecycleProjectionStore projectionStore = new MemOrderLifecycleProjectionStore();
        UUID orderId = UUID.randomUUID();
        OrderLifecycleProjection projection = new OrderLifecycleProjection();
        projection.setOrderId(orderId.toString());
        projectionStore.save(projection);
        outboxRepository.save(OutboxEvent.builder()
                .topic("order.lifecycle")
                .eventKey("BTCUSDT:" + orderId)
                .eventType("OrderLifecycleEvent")
                .build());
        OutboxDomainStateConsistencyService service =
                new OutboxDomainStateConsistencyService(outboxRepository, projectionStore);

        OutboxDomainStateConsistencyReport report = service.inspectLatest(10);

        assertThat(report.scanned()).isEqualTo(1);
        assertThat(report.issues()).isEmpty();
    }

    private static class MemOutboxRepository implements OutboxRepository {
        private final List<OutboxEvent> events = new ArrayList<>();

        @Override
        public void save(OutboxEvent event) {
            events.removeIf(existing -> existing.getId().equals(event.getId()));
            events.add(event);
        }

        @Override
        public Optional<OutboxEvent> findById(UUID id) {
            return events.stream().filter(event -> event.getId().equals(id)).findFirst();
        }

        @Override
        public List<OutboxEvent> findDue(Instant now, int limit) {
            return List.of();
        }

        @Override
        public List<OutboxEvent> latest(int limit) {
            return events.stream()
                    .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                    .limit(Math.max(1, limit))
                    .toList();
        }
    }

    private static class MemOrderLifecycleProjectionStore implements OrderLifecycleProjectionStore {
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
            return List.of();
        }

        @Override
        public List<OrderLifecycleProjection> findByUidAndSymbol(long uid, String symbol) {
            return List.of();
        }

        @Override
        public Optional<OrderLifecycleProjection> findByClientOrderId(String clientOrderId) {
            return Optional.empty();
        }
    }
}
