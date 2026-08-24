package com.lumix.withdrawal.request;

import java.util.Objects;

/** pure request admission result；effective request 永遠是 caller 應保存的 immutable evidence。 */
public record WithdrawalRequestAdmissionResult(
        WithdrawalRequestAdmissionDecision decision,
        WithdrawalRequest effectiveRequest
) {

    public WithdrawalRequestAdmissionResult {
        decision = Objects.requireNonNull(decision, "decision must not be null");
        effectiveRequest = Objects.requireNonNull(effectiveRequest, "effectiveRequest must not be null");
    }
}
