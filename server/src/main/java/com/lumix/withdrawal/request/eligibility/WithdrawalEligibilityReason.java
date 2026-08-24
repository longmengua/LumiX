package com.lumix.withdrawal.request.eligibility;

/** 取得 future hold handoff 前的 deterministic reason。 */
public enum WithdrawalEligibilityReason {
    ELIGIBLE_FOR_FUTURE_HOLD,
    FEE_QUOTE_MISMATCH,
    FEE_QUOTE_EXPIRED,
    INSUFFICIENT_AVAILABLE_BALANCE,
    RISK_REJECTED,
    RISK_UNKNOWN
}
