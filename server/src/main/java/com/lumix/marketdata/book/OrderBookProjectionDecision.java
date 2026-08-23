package com.lumix.marketdata.book;

/**
 * reducer 對單筆已正規化 book event 的唯讀投影決策。
 */
public enum OrderBookProjectionDecision {
    SNAPSHOT_APPLIED,
    DELTA_APPLIED,
    DUPLICATE_IGNORED,
    REJECTED
}
