package com.lumix.trading.core;

/**
 * Trading Runtime Core 的設計範圍枚舉。
 *
 * 這些值只代表 Phase 15 允許談論的 design track，不代表任何 runtime 已經可用。
 */
public enum TradingRuntimeCoreScope {
    LEDGER_POSTING_INTEGRATION_GATE,
    BALANCE_PROJECTION_REBUILD_DESIGN,
    RESERVATION_HOLD_RELEASE_DESIGN,
    RECONCILIATION_DESIGN,
    BASIC_RECONCILIATION_DESIGN
}
