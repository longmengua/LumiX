package com.lumix.marketdata.contract;

import java.time.Instant;

/**
 * Provider-neutral、immutable 的 normalized market-data event envelope。
 *
 * <p>此 record 僅表達已正規化輸入，沒有 processedTimestamp、wall-clock fallback 或任何 runtime side effect。
 * 所有數值與 payload 都在建構時 fail closed，讓未來 replay 可保留原始 identity 與精度。</p>
 */
public record NormalizedMarketDataEvent(
        MarketDataSource source,
        MarketDataChannel channel,
        InstrumentId instrumentId,
        MarketDataEventType eventType,
        Sequence sequence,
        Instant sourceTimestamp,
        Instant receivedTimestamp,
        SchemaVersion schemaVersion,
        InstrumentPrecision precision,
        MarketDataPayload payload
) {

    public NormalizedMarketDataEvent {
        MarketDataContractValidation.requireValue(source, "source");
        MarketDataContractValidation.requireValue(channel, "channel");
        MarketDataContractValidation.requireValue(instrumentId, "instrumentId");
        MarketDataContractValidation.requireValue(eventType, "eventType");
        MarketDataContractValidation.requireValue(sequence, "sequence");
        requireTimestamp(sourceTimestamp, "sourceTimestamp");
        requireTimestamp(receivedTimestamp, "receivedTimestamp");
        MarketDataContractValidation.requireValue(schemaVersion, "schemaVersion");
        MarketDataContractValidation.requireValue(precision, "precision");
        MarketDataContractValidation.requireValue(payload, "payload");

        if (payload.eventType() != eventType) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.PAYLOAD_EVENT_TYPE_MISMATCH,
                    "payload type does not match eventType"
            );
        }
        payload.validateAgainst(precision);
    }

    /**
     * 回傳唯一允許進行 sequence 比較的 stream 範圍；跨 stream 排序由後續 policy 明確拒絕。
     */
    public StreamKey streamKey() {
        return new StreamKey(source, channel, instrumentId);
    }

    /**
     * 回傳 duplicate detection 將使用的 identity 定義，但不在此 task 做 duplicate 決策。
     */
    public MarketDataEventIdentity identity() {
        return new MarketDataEventIdentity(
                source,
                channel,
                instrumentId,
                eventType,
                sequence,
                MarketDataPayloadFingerprint.forPayload(payload)
        );
    }

    private static void requireTimestamp(Instant value, String fieldName) {
        if (value == null) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.MISSING_TIMESTAMP,
                    fieldName + " must not be null"
            );
        }
    }
}
