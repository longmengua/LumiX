package com.lumix.user.auth.domain;

import java.util.Objects;

/** 密碼驗證所需的內部投影；password hash 不得被 controller 或 response 使用。 */
public record PasswordCredential(AuthenticatedUser user, String passwordHash) {

    public PasswordCredential {
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }
}
