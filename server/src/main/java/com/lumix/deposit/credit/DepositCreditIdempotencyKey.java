package com.lumix.deposit.credit;

import java.util.Objects;

/**
 * 入金 credit 的 canonical idempotency identity。
 *
 * <p>key 已納入 network、來源 event、asset、owner 與 policy version；transaction hash 本身絕不能單獨去重。</p>
 */
public record DepositCreditIdempotencyKey(String value) {

    public DepositCreditIdempotencyKey {
        value = Objects.requireNonNull(value, "value must not be null");
        if (!value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("deposit credit idempotency key must be lowercase SHA-256 hex");
        }
    }
}
