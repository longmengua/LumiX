/*
 * 檔案用途：JPA adapter，實作 order lifecycle durable event log 的 domain repository。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.OrderLifecycleEventRecord;
import com.example.exchange.domain.repository.OrderLifecycleEventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaOrderLifecycleEventStore implements OrderLifecycleEventStore {

    private final OrderLifecycleEventRecordJpaRepository repository;

    @Override
    public void append(OrderLifecycleEventRecord record) {
        repository.save(record);
    }

    @Override
    public List<OrderLifecycleEventRecord> findByOrderId(String orderId) {
        return repository.findByOrderIdOrderByEventTsAscIdAsc(orderId);
    }
}
