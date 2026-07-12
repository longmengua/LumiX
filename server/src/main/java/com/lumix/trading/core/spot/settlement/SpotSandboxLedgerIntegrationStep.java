package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement 與 ledger integration 的未來接線順序。
 *
 * 這個順序只是一份設計契約，不能被誤讀成正式 ledger posting integration 已經完成。
 */
public enum SpotSandboxLedgerIntegrationStep {
    /**
     * 先做 idempotency decision。
     */
    IDEMPOTENCY_DECISION,

    /**
     * 再驗證 reservation state。
     */
    RESERVATION_STATE_VERIFICATION,

    /**
     * 再驗證 settlement input。
     */
    SETTLEMENT_INPUT_VALIDATION,

    /**
     * 再檢查 ledger candidate invariant。
     */
    LEDGER_CANDIDATE_INVARIANT_CHECK,

    /**
     * 再進入 human review gate。
     */
    HUMAN_REVIEW_GATE,

    /**
     * 再進入 ledger posting controlled gate。
     */
    LEDGER_POSTING_CONTROLLED_GATE,

    /**
     * 再進入 balance projection refresh gate。
     */
    BALANCE_PROJECTION_REFRESH_GATE,

    /**
     * 再進入 reconciliation check。
     */
    RECONCILIATION_CHECK,

    /**
     * 最後保留 outbox / audit boundary。
     */
    OUTBOX_AUDIT_BOUNDARY
}
