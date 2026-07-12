package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement 的未來 runtime 順序。
 *
 * 這個順序只是一份設計契約，不能被誤讀成正式 settlement runtime 已經完成。
 */
public enum SpotSandboxSettlementStep {
    /**
     * 先做 idempotency decision。
     */
    IDEMPOTENCY_DECISION,

    /**
     * 再驗證 settlement input。
     */
    VALIDATE_SETTLEMENT_INPUT,

    /**
     * 接著確認 reservation state。
     */
    VERIFY_RESERVATION_STATE,

    /**
     * 再計算 base / quote asset movements。
     */
    COMPUTE_ASSET_MOVEMENTS,

    /**
     * 之後才決定 reservation commit / release。
     */
    RESERVATION_COMMIT_RELEASE_DECISION,

    /**
     * 再進入 ledger posting controlled gate。
     */
    LEDGER_POSTING_CONTROLLED_GATE,

    /**
     * 再進入 balance projection refresh gate。
     */
    BALANCE_PROJECTION_REFRESH_GATE,

    /**
     * 最後做 reconciliation check。
     */
    RECONCILIATION_CHECK,

    /**
     * 最後保留 outbox / audit boundary。
     */
    OUTBOX_AUDIT_BOUNDARY
}
