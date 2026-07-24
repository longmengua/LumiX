package com.lumix.marketdata.contract;

/**
 * Normalized market-data contract 的固定拒絕原因。
 *
 * <p>後續 T03 才會決定 duplicate、gap 與 health；本列舉只保護 T02 的輸入契約，
 * 讓呼叫端不必從例外文字推測失敗原因。</p>
 */
public enum MarketDataRejectionReason {
    NULL_VALUE,
    BLANK_IDENTIFIER,
    INVALID_IDENTIFIER,
    INCOMPLETE_STREAM_KEY,
    NON_POSITIVE_SEQUENCE,
    MISSING_TIMESTAMP,
    UNKNOWN_SCHEMA_VERSION,
    UNKNOWN_EVENT_TYPE,
    SCALE_MISMATCH,
    NUMERIC_OVERFLOW,
    INVALID_DECIMAL,
    INVALID_ATOMIC_INTEGER,
    PAYLOAD_EVENT_TYPE_MISMATCH,
    PROVIDER_SPECIFIC_PAYLOAD
}
