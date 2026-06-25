/*
 * 檔案用途：領域 DTO，承載 market-data stream durable sequence checkpoint。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * Market-data sequence checkpoint。
 *
 * @param symbol    normalized symbol code
 * @param stream    stream name, for example DEPTH_DELTA
 * @param sequence  latest durable sequence
 * @param checksum  latest snapshot/delta checksum if available
 * @param updatedAt checkpoint update time
 */
@Data
@Builder
@Jacksonized
public class MarketDataSequenceCheckpoint {

    private final String symbol;

    private final String stream;

    private final long sequence;

    private final long checksum;

    private final Instant updatedAt;
    public MarketDataSequenceCheckpoint(String symbol, String stream, long sequence, long checksum, Instant updatedAt) {
        this.symbol = symbol;
        this.stream = stream;
        this.sequence = sequence;
        this.checksum = checksum;
        this.updatedAt = updatedAt;
    }

    public String symbol() {
        return symbol;
    }

    public String stream() {
        return stream;
    }

    public long sequence() {
        return sequence;
    }

    public long checksum() {
        return checksum;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}