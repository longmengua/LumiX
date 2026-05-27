/*
 * 檔案用途：Spring Data repository，查詢 durable matching engine snapshot。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MatchingEngineSnapshotRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchingEngineSnapshotRecordJpaRepository
        extends JpaRepository<MatchingEngineSnapshotRecord, Long> {

    Optional<MatchingEngineSnapshotRecord> findFirstBySymbolCodeOrderByCommandOffsetDescEventOffsetDescCreatedAtDesc(
            String symbolCode
    );
}
