/*
 * 檔案用途：Spring Data repository，鎖定並查詢 matching sequencer lease record。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MatchingSequencerLeaseRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MatchingSequencerLeaseRecordJpaRepository
        extends JpaRepository<MatchingSequencerLeaseRecord, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select lease from MatchingSequencerLeaseRecord lease where lease.symbolCode = :symbolCode")
    Optional<MatchingSequencerLeaseRecord> findLocked(@Param("symbolCode") String symbolCode);
}
