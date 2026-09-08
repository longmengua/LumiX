package com.lumix.user.auth.domain;

import java.util.Objects;
import java.util.UUID;

/** 已鎖定且尚未消耗的重設憑證；僅在同一個寫入交易中使用。 */
public record ResettableCredential(UUID requestId, AuthenticatedUser user) {

    public ResettableCredential {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(user, "user must not be null");
    }
}
