package com.lumix.deposit.credit.correction;

import com.lumix.deposit.credit.DepositCreditDecisionRecord;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure correction decision output；reversal required 只是一個未來 append-only command 前置條件，不是已執行 reversal。
 */
public record DepositCreditReorgCorrectionPlan(
        DepositCreditReorgDecision decision,
        DepositCreditDecisionRecord originalDecision,
        Optional<AppendOnlyCorrectionOrder> requiredOrder,
        String reason
) {

    public DepositCreditReorgCorrectionPlan {
        decision = Objects.requireNonNull(decision, "decision must not be null");
        originalDecision = Objects.requireNonNull(originalDecision, "originalDecision must not be null");
        requiredOrder = Objects.requireNonNull(requiredOrder, "requiredOrder must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("correction reason must not be blank");
        }
        if (decision == DepositCreditReorgDecision.APPEND_ONLY_REVERSAL_REQUIRED && requiredOrder.isEmpty()) {
            throw new IllegalArgumentException("append-only reversal decision requires explicit ordering evidence");
        }
    }
}
