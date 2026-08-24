package com.lumix.withdrawal.request;

import java.util.Objects;

/** client 提供的 request dedupe key；不得重複使用於不同 payload。 */
public record WithdrawalRequestIdempotencyKey(String value) {

    public WithdrawalRequestIdempotencyKey {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("withdrawal idempotency key must be non-blank and at most 128 characters");
        }
    }
}
