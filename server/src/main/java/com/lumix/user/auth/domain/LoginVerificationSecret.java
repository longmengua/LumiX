package com.lumix.user.auth.domain;

import java.util.Objects;
import java.util.UUID;

/** email 確認頁的一次性登入核准秘密；資料庫只保存摘要。 */
public record LoginVerificationSecret(UUID verificationRequestId, String secret, String secretDigest) {

    public LoginVerificationSecret {
        Objects.requireNonNull(verificationRequestId, "verificationRequestId must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(secretDigest, "secretDigest must not be null");
    }
}
