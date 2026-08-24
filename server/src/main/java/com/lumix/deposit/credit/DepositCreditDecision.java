package com.lumix.deposit.credit;

import java.util.Objects;

/**
 * Pure eligibility decision；eligible 只代表可供下一個經審核的 handoff boundary 使用。
 */
public record DepositCreditDecision(
        DepositCreditDecisionReason reason,
        DepositCreditPolicyVersion policyVersion
) {

    public DepositCreditDecision {
        reason = Objects.requireNonNull(reason, "reason must not be null");
        policyVersion = Objects.requireNonNull(policyVersion, "policyVersion must not be null");
    }

    public boolean eligibleForFutureHandoff() {
        return reason == DepositCreditDecisionReason.ELIGIBLE_FOR_FUTURE_HANDOFF;
    }
}
