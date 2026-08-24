package com.lumix.withdrawal.request;

/** Immutable request admission 的結果；不代表 eligibility、approval 或任何資金移動。 */
public enum WithdrawalRequestAdmissionDecision {
    ACCEPTED_REQUEST,
    DUPLICATE_REPLAY,
    CONFLICTING_PAYLOAD_REJECTED,
    AMOUNT_LIMIT_REJECTED
}
