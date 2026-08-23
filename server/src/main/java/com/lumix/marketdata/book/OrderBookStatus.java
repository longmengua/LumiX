package com.lumix.marketdata.book;

/**
 * 唯讀 order-book projection 的可用狀態。
 *
 * <p>只有 {@link #HEALTHY} 可被後續唯讀 consumer 視為完整 book；保留舊 levels 的非健康狀態
 * 仍不可被宣稱為即時或權威流動性。</p>
 */
public enum OrderBookStatus {
    UNAVAILABLE,
    SYNCING,
    HEALTHY,
    STALE,
    GAP_DETECTED,
    RESYNC_REQUIRED,
    DEGRADED
}
