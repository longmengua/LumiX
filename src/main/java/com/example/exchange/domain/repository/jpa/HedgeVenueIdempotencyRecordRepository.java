/*
 * 檔案用途：JPA Repository，提供 hedge venue idempotency record 查詢與寫入。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.HedgeVenueIdempotencyRecordEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HedgeVenueIdempotencyRecordRepository
        extends JpaRepository<HedgeVenueIdempotencyRecordEntity, Long> {

    Optional<HedgeVenueIdempotencyRecordEntity> findByRefId(String refId);

    List<HedgeVenueIdempotencyRecordEntity> findByCompletedFalseOrRetryableTrueOrderByUpdatedAtAsc(Pageable pageable);
}
