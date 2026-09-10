package com.lumix.user.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 持久化登入驗證請求的 application 投影，不含任何原始 token。 */
public record LoginVerificationRequest(
    UUID verificationRequestId,
    AuthenticatedUser user,
    UUID candidateDeviceId,
    String candidateDeviceTokenDigest,
    LoginRequestMetadata metadata,
    LoginVerificationState state,
    Instant expiresAt
) {

    public LoginVerificationRequest {
        Objects.requireNonNull(verificationRequestId, "verificationRequestId must not be null");
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(candidateDeviceId, "candidateDeviceId must not be null");
        Objects.requireNonNull(candidateDeviceTokenDigest, "candidateDeviceTokenDigest must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
