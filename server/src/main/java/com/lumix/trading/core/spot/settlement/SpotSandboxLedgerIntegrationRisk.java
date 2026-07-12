package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement 與 ledger integration 的風險類型。
 *
 * 這些風險只用來標示 design gate 必須特別 review 的點，不代表正式 runtime 行為。
 */
public enum SpotSandboxLedgerIntegrationRisk {
    /**
     * 重複 settlement。
     */
    DUPLICATE_SETTLEMENT,

    /**
     * ledger append 失敗。
     */
    LEDGER_APPEND_FAILURE,

    /**
     * balance refresh 延遲。
     */
    BALANCE_REFRESH_LAG,

    /**
     * reservation commit mismatch。
     */
    RESERVATION_COMMIT_MISMATCH,

    /**
     * reconciliation mismatch。
     */
    RECONCILIATION_MISMATCH
}
