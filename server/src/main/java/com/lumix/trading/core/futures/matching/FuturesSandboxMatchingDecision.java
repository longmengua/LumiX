package com.lumix.trading.core.futures.matching;

/**
 * Futures sandbox matching reuse gate 的結果類型。
 *
 * MATCH_ELIGIBLE 只表示 accepted snapshots 的限價條件可配對，不表示已撮合、已成交或已更新任何後續領域狀態。
 */
public enum FuturesSandboxMatchingDecision {
    MATCH_ELIGIBLE,
    NO_CROSS,
    REJECTED
}
