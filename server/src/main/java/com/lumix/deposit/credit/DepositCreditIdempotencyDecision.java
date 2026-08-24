package com.lumix.deposit.credit;

/**
 * 對 immutable idempotency evidence 的 pure 判定；不代表資料庫鎖定或 transaction 已完成。
 */
public enum DepositCreditIdempotencyDecision {
    NEW_RECORD,
    DUPLICATE_REPLAY,
    CONFLICTING_PAYLOAD_REJECTED,
    INELIGIBLE_REJECTED
}
