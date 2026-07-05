package com.lumix.common;

import java.util.Objects;

/**
 * 請求識別碼。
 * 用於 transfer、ledger、idempotency 等高風險流程的唯一追蹤。
 */
public record RequestId(String value) {

    public RequestId {
        // RequestId 是跨服務追蹤的主鍵，必須先確認內容非空白。
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
    }
}
