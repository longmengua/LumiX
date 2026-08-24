package com.lumix.withdrawal.request.eligibility;

import java.util.Objects;
import java.util.Optional;

/** 只有 eligible 結果才可帶 future hold handoff。 */
public record WithdrawalEligibilityResult(WithdrawalEligibilityReason reason, Optional<WithdrawalHoldHandoff> holdHandoff) {
    public WithdrawalEligibilityResult {
        reason = Objects.requireNonNull(reason, "reason must not be null");
        holdHandoff = Objects.requireNonNull(holdHandoff, "holdHandoff must not be null");
        if ((reason == WithdrawalEligibilityReason.ELIGIBLE_FOR_FUTURE_HOLD) != holdHandoff.isPresent()) {
            throw new IllegalArgumentException("hold handoff must exactly match eligible reason");
        }
    }
}
