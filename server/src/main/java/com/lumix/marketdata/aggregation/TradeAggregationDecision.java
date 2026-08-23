package com.lumix.marketdata.aggregation;

/**
 * trade aggregation 對單筆 event 的唯讀 transition 結果。
 */
public enum TradeAggregationDecision {
    APPLIED,
    DUPLICATE_IGNORED,
    REJECTED
}
