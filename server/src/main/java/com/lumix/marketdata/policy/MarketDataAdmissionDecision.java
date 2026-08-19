package com.lumix.marketdata.policy;

/**
 * 單一 normalized event 對指定 stream cursor 的受理結果。
 *
 * <p>這個結果只決定事件能否交給未來的唯讀 projection；不會套用 book、更新 cache、
 * 發出 resync 請求或操作任何交易核心狀態。</p>
 */
public enum MarketDataAdmissionDecision {
    ACCEPTED,
    DUPLICATE_IGNORED,
    OUT_OF_ORDER_REJECTED,
    GAP_DETECTED,
    RESYNC_REQUIRED,
    INTEGRITY_CONFLICT,
    STREAM_MISMATCH_REJECTED,
    STOPPED
}
