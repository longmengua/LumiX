package com.lumix.account;

import java.util.Objects;

/**
 * 帳戶識別碼。
 * 只表示帳戶 ID，本身不含任何餘額或權限邏輯。
 */
public record AccountId(String value) {

    public AccountId {
        // 帳戶 ID 是高頻查詢鍵，建立時直接阻擋空值。
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
    }
}
