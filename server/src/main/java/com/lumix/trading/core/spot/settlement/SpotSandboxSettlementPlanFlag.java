package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement plan 的旗標。
 *
 * 這些旗標只用來標示尚未完成的下游 gate，避免把 plan 誤讀成已經 settlement 完成。
 */
public enum SpotSandboxSettlementPlanFlag {
    /**
     * ledger 尚未 post。
     */
    LEDGER_NOT_POSTED,

    /**
     * balance projection 尚未更新。
     */
    BALANCE_NOT_UPDATED,

    /**
     * reservation 尚未 commit。
     */
    RESERVATION_NOT_COMMITTED,

    /**
     * reconciliation 尚未完成。
     */
    RECONCILIATION_NOT_COMPLETED
}
