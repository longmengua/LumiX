package com.lumix.user.auth.domain;

import java.util.Objects;
import java.util.UUID;

/** 一次性重設 token 的短暫內部表示，原始 token 僅能交給寄信 adapter。 */
public record PasswordResetSecret(UUID requestId, String secret, String secretDigest) {

    public PasswordResetSecret {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(secretDigest, "secretDigest must not be null");
    }
}
