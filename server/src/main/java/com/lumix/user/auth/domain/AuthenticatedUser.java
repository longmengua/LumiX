package com.lumix.user.auth.domain;

import java.util.Objects;

/** 已通過 session 驗證的最小使用者投影，避免把 credential 資料傳到 API 層。 */
public record AuthenticatedUser(String userId, String email, String displayName) {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
    }
}
