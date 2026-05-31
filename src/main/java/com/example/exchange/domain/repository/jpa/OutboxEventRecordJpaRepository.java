/*
 * 檔案用途：Spring Data JPA repository，提供 durable outbox event 存取。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.OutboxEventRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRecordJpaRepository
        extends JpaRepository<OutboxEventRecord, String> {

    @Query("""
            select event
            from OutboxEventRecord event
            where event.status = 'PENDING'
              and (event.nextAttemptAt is null or event.nextAttemptAt <= :now)
            order by event.nextAttemptAt asc, event.createdAt asc
            """)
    List<OutboxEventRecord> findDue(@Param("now") Instant now, Pageable pageable);

    List<OutboxEventRecord> findByOrderByCreatedAtDesc(Pageable pageable);
}
