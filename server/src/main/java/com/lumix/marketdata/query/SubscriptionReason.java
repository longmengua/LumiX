package com.lumix.marketdata.query;

/** 固定原因碼供 consumer metrics 與後續 transport adapter 保留失敗語意。 */
public enum SubscriptionReason {
    CONTIGUOUS_VERSION,
    DUPLICATE_OR_OLDER_VERSION,
    STREAM_KEY_MISMATCH,
    NON_HEALTHY_VIEW,
    VERSION_GAP,
    CONSUMER_CAPACITY_EXCEEDED
}
