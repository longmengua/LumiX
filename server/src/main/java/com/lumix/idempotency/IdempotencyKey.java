package com.lumix.idempotency;

import java.util.Objects;

/**
 * 冪等鍵。
 * 只負責 key 的格式驗證，不接任何 Redis 或 DB 實作。
 */
public record IdempotencyKey(String value) {

    public IdempotencyKey {
        // 冪等鍵必須可用於查詢與去重，因此不接受空白字串。
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
