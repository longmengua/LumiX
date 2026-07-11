package com.lumix.ledger.application.idempotency;

/**
 * ledger idempotency contract 的 scope。
 *
 * 這裡先對齊 Phase 12 的 idempotency scope 語意，僅保留 ledger posting 所需的設計範圍。
 */
public enum LedgerIdempotencyScope {
    LEDGER_POSTING
}
