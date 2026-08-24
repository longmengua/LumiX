package com.lumix.withdrawal.signing;

import java.util.Objects;
import java.util.Optional;

/** immutable intent policy output；reject 時不提供任何能被 signer 消費的 intent。 */
public record WithdrawalSigningIntentResult(WithdrawalSigningIntentDecision decision, Optional<WithdrawalSigningIntent> signingIntent) {
    public WithdrawalSigningIntentResult {
        decision = Objects.requireNonNull(decision, "decision");
        signingIntent = Objects.requireNonNull(signingIntent, "signingIntent");
    }
}
