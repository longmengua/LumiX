/*
 * 檔案用途：應用服務，管理 market-data stream 的 durable sequence checkpoint。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.MarketDataSequenceCheckpoint;
import com.example.exchange.domain.repository.MarketDataSequenceCheckpointStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketDataSequenceCheckpointService {

    public static final String DEPTH_DELTA_STREAM = "DEPTH_DELTA";

    private final MarketDataSequenceCheckpointStore store;

    public MarketDataSequenceCheckpoint advance(
            String symbol,
            String stream,
            long sequence,
            long checksum,
            Instant updatedAt
    ) {
        String normalizedSymbol = normalize(symbol);
        String normalizedStream = normalize(stream);
        if (sequence <= 0) {
            throw new IllegalArgumentException("market-data sequence must be positive");
        }
        MarketDataSequenceCheckpoint current = store.find(normalizedSymbol, normalizedStream).orElse(null);
        if (current != null && sequence <= current.sequence()) {
            return current;
        }
        return store.save(new MarketDataSequenceCheckpoint(
                normalizedSymbol,
                normalizedStream,
                sequence,
                checksum,
                updatedAt == null ? Instant.now() : updatedAt
        ));
    }

    public Optional<MarketDataSequenceCheckpoint> latest(String symbol, String stream) {
        return store.find(normalize(symbol), normalize(stream));
    }

    public long latestSequence(String symbol, String stream) {
        return latest(symbol, stream)
                .map(MarketDataSequenceCheckpoint::sequence)
                .orElse(0L);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
