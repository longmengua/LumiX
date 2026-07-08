package com.lumix.ledger.application.transaction;

/**
 * ledger append transaction 的設計步驟。
 *
 * 這些 step 只描述未來單一 transaction 內應包含的順序，不代表已經完成實作。
 */
public enum LedgerAppendTransactionStep {
    IDEMPOTENCY_CHECK_OR_LOCK,
    JOURNAL_HEADER_APPEND,
    JOURNAL_ENTRIES_APPEND,
    OUTBOX_APPEND,
    AUDIT_APPEND,
    COMMIT
}
