package com.lumix.withdrawal.signing;

/** 建立 signing intent 的結果；任何 reject 都不保留或觸發 signer 操作。 */
public enum WithdrawalSigningIntentDecision {
    INTENT_CREATED,
    DUPLICATE_REPLAY,
    APPROVAL_NOT_GRANTED_REJECTED,
    CONFLICTING_IDEMPOTENCY_PAYLOAD_REJECTED
}
