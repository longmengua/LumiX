/*
 * 檔案用途：Spring Data JPA repository，提供 market-data kline 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.entity.MarketDataKlineRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MarketDataKlineRecordJpaRepository
        extends JpaRepository<MarketDataKlineRecord, MarketDataKlineRecord.Key> {

    List<MarketDataKlineRecord> findBySymbolAndIntervalOrderByOpenTimeDesc(
            String symbol,
            String interval,
            Pageable pageable
    );

    java.util.Optional<MarketDataKlineRecord> findBySymbolAndIntervalAndOpenTime(
            String symbol,
            String interval,
            Instant openTime
    );

    long deleteByOpenTimeBefore(Instant cutoff);
}
