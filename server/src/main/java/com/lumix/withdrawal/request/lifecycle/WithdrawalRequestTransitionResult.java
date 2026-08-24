package com.lumix.withdrawal.request.lifecycle;

import java.util.Objects;

/** pure transition result；effective state 是唯一可保存的下一份 immutable evidence。 */
public record WithdrawalRequestTransitionResult(WithdrawalRequestTransitionDecision decision, WithdrawalRequestState effectiveState) {
    public WithdrawalRequestTransitionResult {
        decision = Objects.requireNonNull(decision, "decision must not be null");
        effectiveState = Objects.requireNonNull(effectiveState, "effectiveState must not be null");
    }
}
