/*
 * 檔案用途：JPA adapter，保存與查詢做市商 quote active-state。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MarketMakerQuoteState;
import com.example.exchange.domain.model.entity.MarketMakerQuoteStateRecord;
import com.example.exchange.domain.repository.MarketMakerQuoteStateStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMarketMakerQuoteStateStore implements MarketMakerQuoteStateStore {

    private final MarketMakerQuoteStateRecordJpaRepository repository;

    @Override
    @Transactional
    public void save(MarketMakerQuoteState state) {
        repository.save(MarketMakerQuoteStateRecord.from(state));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketMakerQuoteState> find(String marketMakerId, String symbol) {
        return repository.findByMarketMakerIdAndSymbol(normalizeMarketMakerId(marketMakerId), normalizeSymbol(symbol))
                .map(MarketMakerQuoteStateRecord::toState);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketMakerQuoteState> findByMarketMakerId(String marketMakerId, int limit) {
        return repository.findByMarketMakerIdOrderByUpdatedAtDesc(
                        normalizeMarketMakerId(marketMakerId),
                        PageRequest.of(0, safeLimit(limit))
                )
                .stream()
                .map(MarketMakerQuoteStateRecord::toState)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarketMakerQuoteState> findActive(int limit) {
        return repository.findByActiveTrueOrderByUpdatedAtDesc(PageRequest.of(0, safeLimit(limit)))
                .stream()
                .map(MarketMakerQuoteStateRecord::toState)
                .toList();
    }

    private static String normalizeMarketMakerId(String marketMakerId) {
        if (marketMakerId == null || marketMakerId.isBlank()) {
            throw new IllegalArgumentException("market maker id is required");
        }
        return marketMakerId.trim();
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        return symbol.trim().toUpperCase();
    }

    private static int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }
}
