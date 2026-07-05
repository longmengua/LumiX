package com.lumix.account;

import java.util.Objects;

/**
 * 使用者識別碼。
 * 只做身分識別，不承載任何帳戶資料。
 */
public record UserId(String value) {

    public UserId {
        // userId 會貫穿帳戶、帳本、冪等等模組，空值會讓追蹤失效。
        Objects.requireNonNull(value, "value must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }
}
