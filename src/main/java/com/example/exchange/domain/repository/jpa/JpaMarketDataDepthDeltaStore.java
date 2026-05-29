/*
 * 檔案用途：JPA adapter，實作 depth delta durable store / reconnect backfill 查詢。
 */
package com.example.exchange.domain.repository.jpa;

import com.example.exchange.domain.model.dto.DepthDelta;
import com.example.exchange.domain.model.dto.PriceLevel;
import com.example.exchange.domain.model.entity.MarketDataDepthDeltaRecord;
import com.example.exchange.domain.repository.MarketDataDepthDeltaStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaMarketDataDepthDeltaStore implements MarketDataDepthDeltaStore {

    private static final TypeReference<List<PriceLevel>> PRICE_LEVELS = new TypeReference<>() {
    };

    private final MarketDataDepthDeltaRecordJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void append(DepthDelta delta) {
        repository.save(toRecord(delta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepthDelta> findAfter(String symbol, long afterVersion, int limit) {
        int normalizedLimit = Math.min(Math.max(1, limit), 1000);
        return repository.findBySymbolAndVersionGreaterThanOrderByVersionAsc(
                        normalize(symbol),
                        afterVersion,
                        PageRequest.of(0, normalizedLimit)
                )
                .stream()
                .map(this::toDelta)
                .toList();
    }

    @Override
    @Transactional
    public long purgeBefore(Instant cutoff) {
        return cutoff == null ? 0L : repository.deleteByCreatedAtBefore(cutoff);
    }

    private MarketDataDepthDeltaRecord toRecord(DepthDelta delta) {
        MarketDataDepthDeltaRecord record = new MarketDataDepthDeltaRecord();
        record.setSymbol(normalize(delta.symbol()));
        record.setVersion(delta.version());
        record.setChecksum(delta.checksum());
        record.setBidsJson(write(delta.bids()));
        record.setAsksJson(write(delta.asks()));
        record.setCreatedAt(delta.ts());
        return record;
    }

    private DepthDelta toDelta(MarketDataDepthDeltaRecord record) {
        return new DepthDelta(
                record.getSymbol(),
                record.getVersion(),
                record.getChecksum(),
                read(record.getBidsJson()),
                read(record.getAsksJson()),
                record.getCreatedAt()
        );
    }

    private String write(List<PriceLevel> levels) {
        try {
            return objectMapper.writeValueAsString(levels == null ? List.of() : levels);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to serialize depth delta levels", ex);
        }
    }

    private List<PriceLevel> read(String json) {
        try {
            return objectMapper.readValue(json == null || json.isBlank() ? "[]" : json, PRICE_LEVELS);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to deserialize depth delta levels", ex);
        }
    }

    private static String normalize(String symbol) {
        return symbol == null ? "" : symbol.trim().toUpperCase();
    }
}
