/*
 * 檔案用途：JPA adapter，實作 market ticker latest-state store。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.MarketTicker;
import com.example.exchange.domain.model.entity.MarketDataTickerRecord;
import com.example.exchange.domain.repository.MarketDataTickerStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaMarketDataTickerStore implements MarketDataTickerStore {

    private final MarketDataTickerRecordJpaRepository repository;

    @Override
    @Transactional
    public void save(MarketTicker ticker) {
        repository.save(MarketDataTickerRecord.from(ticker));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MarketTicker> find(String symbol) {
        return repository.findById(normalize(symbol))
                .map(MarketDataTickerRecord::toTicker);
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
