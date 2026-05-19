/*
 * 檔案用途：Spring Data JPA repository，提供 order lifecycle event log 的資料庫存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.OrderLifecycleEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderLifecycleEventRecordJpaRepository
        extends JpaRepository<OrderLifecycleEventRecord, Long> {

    List<OrderLifecycleEventRecord> findByOrderIdOrderByEventTsAscIdAsc(String orderId);
}
