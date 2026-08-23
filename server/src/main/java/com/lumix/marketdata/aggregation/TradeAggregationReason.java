package com.lumix.marketdata.aggregation;

/**
 * 固定 aggregation 原因碼，供後續 metrics、audit evidence 與 recovery policy 使用。
 */
public enum TradeAggregationReason {
    TRADE_APPLIED,
    DUPLICATE_ADMISSION,
    ADMISSION_NOT_ACCEPTED,
    ADMISSION_EVENT_MISMATCH,
    NON_TRADE_STREAM,
    NON_TRADE_EVENT,
    STREAM_KEY_MISMATCH,
    FEED_NOT_HEALTHY,
    PROJECTION_IDENTITY_ALREADY_APPLIED,
    SEQUENCE_NOT_CONTINUOUS,
    LATE_SOURCE_EVENT,
    WINDOW_TRADE_LIMIT_EXCEEDED,
    NUMERIC_OVERFLOW
}
