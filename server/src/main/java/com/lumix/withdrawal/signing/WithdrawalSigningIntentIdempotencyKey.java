package com.lumix.withdrawal.signing;

import java.util.Objects;

/** request-to-sign 的 client retry key；只能在單一 withdrawal request 範圍內比較。 */
public record WithdrawalSigningIntentIdempotencyKey(String value) {
    public WithdrawalSigningIntentIdempotencyKey {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("signing intent idempotency key must not be blank");
        }
    }
}
