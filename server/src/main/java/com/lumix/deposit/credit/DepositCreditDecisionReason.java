package com.lumix.deposit.credit;

/**
 * 對未來 handoff eligibility 的可審計原因；沒有任何 reason 可直接觸發 credit。
 */
public enum DepositCreditDecisionReason {
    ELIGIBLE_FOR_FUTURE_HANDOFF,
    POLICY_DISABLED,
    POLICY_VERSION_MISMATCH,
    NETWORK_MISMATCH,
    ASSET_MISMATCH,
    RECIPIENT_ADDRESS_MISMATCH,
    OWNERSHIP_NOT_ACTIVE,
    FINALITY_NOT_MET,
    INSUFFICIENT_CONFIRMATIONS
}
