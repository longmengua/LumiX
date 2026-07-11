package com.lumix.trading.core.posting;

/**
 * ledger posting integration 的設計步驟。
 *
 * 這些步驟只描述未來正式接線時必須遵守的順序，不代表任何 runtime 已完成。
 */
public enum LedgerPostingIntegrationStep {
    REQUEST_IDENTITY_AND_IDEMPOTENCY,
    PREREQUISITE_GATE,
    LEDGER_INVARIANT_CHECK,
    APPEND_TRANSACTION_BOUNDARY,
    APPEND_LEDGER_ROWS,
    OUTBOX_APPEND,
    AUDIT_APPEND,
    RECONCILIATION_MARKER
}
