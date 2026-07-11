package com.lumix.trading.core.spot;

/**
 * Spot sandbox 目前允許或預留的 boundary 能力。
 *
 * 這些能力只代表 sandbox 路線上的設計邊界，不代表任何正式交易 runtime 已完成。
 */
public enum SpotSandboxCapability {
    SCOPE_GATE,
    ORDER_INTAKE_BOUNDARY,
    RESERVATION_BOUNDARY,
    MATCHING_BOUNDARY,
    SETTLEMENT_BOUNDARY,
    LEDGER_POSTING_GATE,
    BALANCE_PROJECTION_REBUILD_GATE,
    RECONCILIATION_BOUNDARY
}
