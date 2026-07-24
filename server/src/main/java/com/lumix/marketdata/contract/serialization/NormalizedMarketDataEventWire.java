package com.lumix.marketdata.contract.serialization;

/**
 * Normalized event 的 stable serialization mapping。
 *
 * <p>時間採 ISO-8601 {@code Instant} 字串，sequence、schema version、價格與數量均採明確字串或整數欄位；
 * 不存在 processedTimestamp，也不由 mapping 讀取 clock。</p>
 */
public record NormalizedMarketDataEventWire(
        String source,
        String channel,
        String instrumentId,
        String eventType,
        String sequence,
        String sourceTimestamp,
        String receivedTimestamp,
        String schemaVersion,
        int priceScale,
        int quantityScale,
        int maximumSignificantDigits,
        MarketDataPayloadWire payload
) {
}
