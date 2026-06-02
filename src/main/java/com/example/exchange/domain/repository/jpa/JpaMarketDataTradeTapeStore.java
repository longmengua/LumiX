/*
 * 檔案用途：JPA adapter，實作 market-data trade tape store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.TradeTapeItem;
import com.example.exchange.domain.model.entity.MarketDataTradeTapeRecord;
import com.example.exchange.domain.repository.MarketDataTradeTapeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaMarketDataTradeTapeStore implements MarketDataTradeTapeStore {

    private final MarketDataTradeTapeRecordJpaRepository repository;

    @Override
    @Transactional
    public void append(TradeTapeItem item) {
        repository.save(MarketDataTradeTapeRecord.from(item));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeTapeItem> findRecent(String symbol, int limit) {
        int normalizedLimit = Math.min(Math.max(1, limit), 1000);
        return repository.findBySymbolOrderByTradeTsDescIdDesc(
                        normalize(symbol),
                        PageRequest.of(0, normalizedLimit)
                )
                .stream()
                .map(MarketDataTradeTapeRecord::toItem)
                .sorted(Comparator.comparing(TradeTapeItem::ts).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeTapeItem> findAfter(String symbol, Instant afterTs, String afterMatchId, int limit) {
        if (afterTs == null) {
            return findRecent(symbol, limit);
        }
        int normalizedLimit = Math.min(Math.max(1, limit), 1000);
        String normalizedMatchId = afterMatchId == null || afterMatchId.isBlank() ? null : afterMatchId.trim();
        return repository.findAfterCursor(
                        normalize(symbol),
                        afterTs,
                        normalizedMatchId,
                        PageRequest.of(0, normalizedLimit)
                )
                .stream()
                .map(MarketDataTradeTapeRecord::toItem)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TradeTapeItem> findByMatchId(String matchId) {
        if (matchId == null || matchId.isBlank()) return List.of();
        return repository.findByMatchIdOrderByTradeTsAscIdAsc(matchId.trim())
                .stream()
                .map(MarketDataTradeTapeRecord::toItem)
                .toList();
    }

    @Override
    @Transactional
    public long purgeBefore(Instant cutoff) {
        return cutoff == null ? 0L : repository.deleteByTradeTsBefore(cutoff);
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
