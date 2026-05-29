/*
 * 檔案用途：JPA adapter，實作 market-data kline store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MarketKline;
import com.example.exchange.domain.model.entity.MarketDataKlineRecord;
import com.example.exchange.domain.repository.MarketDataKlineStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMarketDataKlineStore implements MarketDataKlineStore {

    private final MarketDataKlineRecordJpaRepository repository;

    @Override
    @Transactional
    public MarketKline save(MarketKline kline) {
        return repository.save(MarketDataKlineRecord.from(kline)).toKline();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketKline> find(String symbol, String interval, Instant openTime) {
        return repository.findBySymbolAndIntervalAndOpenTime(
                        normalize(symbol),
                        normalizeInterval(interval),
                        openTime
                )
                .map(MarketDataKlineRecord::toKline);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketKline> findRecent(String symbol, String interval, int limit) {
        int normalizedLimit = Math.min(Math.max(1, limit), 1000);
        return repository.findBySymbolAndIntervalOrderByOpenTimeDesc(
                        normalize(symbol),
                        normalizeInterval(interval),
                        PageRequest.of(0, normalizedLimit)
                )
                .stream()
                .map(MarketDataKlineRecord::toKline)
                .sorted(Comparator.comparing(MarketKline::openTime).reversed())
                .toList();
    }

    @Override
    @Transactional
    public long purgeBefore(Instant cutoff) {
        return cutoff == null ? 0L : repository.deleteByOpenTimeBefore(cutoff);
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }

    private static String normalizeInterval(String interval) {
        return interval == null ? "" : interval.trim().toLowerCase();
    }
}
