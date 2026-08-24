package com.lumix.withdrawal.signing;

import java.time.Duration;
import java.util.Objects;

/** 未來 isolated adapter 的最小輸入封套；只允許 intent digest、capability identity 與明確 timeout/retry 上限。 */
public record WithdrawalSignerDispatchEnvelope(WithdrawalSigningIntent signingIntent, String capabilityId, Duration timeout, int maximumAttempts) {
    public WithdrawalSignerDispatchEnvelope {
        signingIntent = Objects.requireNonNull(signingIntent, "signingIntent"); capabilityId = Objects.requireNonNull(capabilityId, "capabilityId").trim(); timeout = Objects.requireNonNull(timeout, "timeout");
        if (capabilityId.isEmpty() || timeout.isNegative() || timeout.isZero() || maximumAttempts < 1) throw new IllegalArgumentException("dispatch limits must be positive");
    }
}
