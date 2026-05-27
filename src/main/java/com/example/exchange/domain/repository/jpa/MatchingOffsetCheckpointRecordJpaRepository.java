/*
 * 檔案用途：Spring Data repository，鎖定並推進 matching durable offset checkpoint。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MatchingOffsetCheckpointRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MatchingOffsetCheckpointRecordJpaRepository
        extends JpaRepository<MatchingOffsetCheckpointRecord, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select checkpoint from MatchingOffsetCheckpointRecord checkpoint where checkpoint.symbolCode = :symbolCode")
    Optional<MatchingOffsetCheckpointRecord> findLocked(@Param("symbolCode") String symbolCode);
}
