/*
 * 檔案用途：Spring Data JPA repository，提供 depth delta backfill 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketDataDepthDeltaRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MarketDataDepthDeltaRecordJpaRepository
        extends JpaRepository<MarketDataDepthDeltaRecord, MarketDataDepthDeltaRecord.Key> {

    List<MarketDataDepthDeltaRecord> findBySymbolAndVersionGreaterThanOrderByVersionAsc(
            String symbol,
            Long version,
            Pageable pageable
    );

    long deleteByCreatedAtBefore(Instant cutoff);
}
