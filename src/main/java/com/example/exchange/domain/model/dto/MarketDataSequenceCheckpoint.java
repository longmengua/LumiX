/*
 * 檔案用途：領域 DTO，承載 market-data stream durable sequence checkpoint。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;

/**
 * Market-data sequence checkpoint。
 *
 * @param symbol    normalized symbol code
 * @param stream    stream name, for example DEPTH_DELTA
 * @param sequence  latest durable sequence
 * @param checksum  latest snapshot/delta checksum if available
 * @param updatedAt checkpoint update time
 */
public record MarketDataSequenceCheckpoint(
        String symbol,
        String stream,
        long sequence,
        long checksum,
        Instant updatedAt
) {
}
