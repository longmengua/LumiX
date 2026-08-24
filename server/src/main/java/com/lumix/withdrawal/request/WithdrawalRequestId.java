package com.lumix.withdrawal.request;

import java.util.Objects;

/** 提款請求的 audit identity，不可由 destination 或 transaction hash 取代。 */
public record WithdrawalRequestId(String value) {

    public WithdrawalRequestId {
        value = Objects.requireNonNull(value, "value must not be null").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("withdrawal request id must not be blank");
        }
    }
}
