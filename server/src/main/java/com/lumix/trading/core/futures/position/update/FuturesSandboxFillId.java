package com.lumix.trading.core.futures.position.update;

import java.util.Objects;

/**
 * Futures sandbox verified fill 的穩定識別碼。
 *
 * 此 ID 供 position update 的 idempotency contract 使用；它不代表已被資料庫或正式 trade store 持久化。
 */
public record FuturesSandboxFillId(String value) {

    public FuturesSandboxFillId {
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("futuresSandboxFillId must not be blank");
        }
    }
}
