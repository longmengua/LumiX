/*
 * 檔案用途：應用服務，寫入並查詢 order lifecycle durable event log 與最新狀態 projection。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.event.OrderLifecycleEvent;
import com.example.exchange.domain.model.entity.OrderLifecycleEventRecord;
import com.example.exchange.domain.model.entity.OrderLifecycleProjection;
import com.example.exchange.domain.repository.OrderLifecycleEventStore;
import com.example.exchange.domain.repository.OrderLifecycleProjectionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderLifecycleProjectionService {

    public static final int SCHEMA_VERSION = 1;

    private final OrderLifecycleEventStore eventStore;
    private final OrderLifecycleProjectionStore projectionStore;

    @Transactional
    public void record(OrderLifecycleEvent event) {
        if (event == null || event.orderId() == null || event.stage() == null || event.status() == null) {
            return;
        }

        OrderLifecycleEventRecord record = OrderLifecycleEventRecord.from(event, SCHEMA_VERSION);
        eventStore.append(record);
        applyToProjection(record);
    }

    @Transactional
    public Optional<OrderLifecycleProjection> rebuildProjection(UUID orderId) {
        if (orderId == null) return Optional.empty();
        List<OrderLifecycleEventRecord> history = eventStore.findByOrderId(orderId.toString());
        if (history.isEmpty()) return Optional.empty();

        OrderLifecycleProjection projection = new OrderLifecycleProjection();
        history.forEach(projection::apply);
        projectionStore.save(projection);
        return Optional.of(projection);
    }

    @Transactional(readOnly = true)
    public List<OrderLifecycleEventRecord> history(UUID orderId) {
        if (orderId == null) return List.of();
        return eventStore.findByOrderId(orderId.toString());
    }

    @Transactional(readOnly = true)
    public Optional<OrderLifecycleProjection> projection(UUID orderId) {
        if (orderId == null) return Optional.empty();
        return projectionStore.findByOrderId(orderId.toString());
    }

    @Transactional(readOnly = true)
    public List<OrderLifecycleProjection> projections(long uid, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return projectionStore.findByUid(uid);
        }
        return projectionStore.findByUidAndSymbol(uid, symbol.trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public Optional<OrderLifecycleProjection> projectionByClientOrderId(String clientOrderId) {
        if (clientOrderId == null || clientOrderId.isBlank()) return Optional.empty();
        return projectionStore.findByClientOrderId(clientOrderId.trim());
    }

    private void applyToProjection(OrderLifecycleEventRecord record) {
        OrderLifecycleProjection projection = projectionStore.findByOrderId(record.getOrderId())
                .orElseGet(OrderLifecycleProjection::new);
        projection.apply(record);
        projectionStore.save(projection);
    }
}
