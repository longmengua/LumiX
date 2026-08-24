package com.lumix.withdrawal.signing;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** P25-T03 僅驗證 capability 與限制後產生封套，不提供 signer adapter、retry loop 或任何 secret handling。 */
public final class WithdrawalSignerDispatchPolicy {
    public Result evaluate(WithdrawalSigningIntent intent, WithdrawalSignerCapability capability, Duration timeout, int maximumAttempts) {
        intent = Objects.requireNonNull(intent, "intent"); capability = Objects.requireNonNull(capability, "capability");
        if (!capability.available()) return new Result(WithdrawalSignerDispatchDecision.CAPABILITY_UNAVAILABLE_REJECTED, Optional.empty());
        if (!capability.supports(intent.signerInput().requestState().request().network())) return new Result(WithdrawalSignerDispatchDecision.NETWORK_UNSUPPORTED_REJECTED, Optional.empty());
        return new Result(WithdrawalSignerDispatchDecision.READY_FOR_ISOLATED_ADAPTER, Optional.of(new WithdrawalSignerDispatchEnvelope(intent, capability.capabilityId(), timeout, maximumAttempts)));
    }
    /** reject 時沒有封套，避免上游把未驗證 capability 當作可簽章工作。 */
    public record Result(WithdrawalSignerDispatchDecision decision, Optional<WithdrawalSignerDispatchEnvelope> envelope) {
        public Result { decision = Objects.requireNonNull(decision, "decision"); envelope = Objects.requireNonNull(envelope, "envelope"); }
    }
}
