package com.lumix.marketdata.policy;

/**
 * 可供指標與審計使用的固定 admission 原因碼。
 *
 * <p>不使用自由文字作為流程判斷，讓 replay、計數器與後續告警可以穩定地區分 duplicate、
 * gap、完整性衝突及 resync 等情況。</p>
 */
public enum MarketDataAdmissionReason {
    INITIAL_BASELINE_ACCEPTED,
    CONTIGUOUS_SEQUENCE_ACCEPTED,
    RESYNC_SNAPSHOT_ACCEPTED,
    STALE_EVENT_ACCEPTED,
    DUPLICATE_IDENTITY,
    OUT_OF_ORDER_SEQUENCE,
    SEQUENCE_GAP,
    INITIAL_BOOK_DELTA_REQUIRES_SNAPSHOT,
    RESYNC_PENDING,
    CONFLICTING_PAYLOAD_FOR_SEQUENCE,
    STREAM_KEY_MISMATCH,
    STREAM_STOPPED
}
