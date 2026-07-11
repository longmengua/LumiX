package com.lumix.ledger.application.idempotency;

/**
 * ledger idempotency design 的決策狀態。
 *
 * 這些值只描述未來 runtime 可能遇到的判定結果，不代表已完成 DB lookup、lock 或 replay。
 */
public enum LedgerIdempotencyDecision {
    NEW_REQUEST,
    DUPLICATE_COMPLETED,
    IN_PROGRESS,
    CONFLICTING_REQUEST,
    EXPIRED_OR_RETRY_REQUIRED
}
