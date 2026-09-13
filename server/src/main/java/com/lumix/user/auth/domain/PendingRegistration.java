package com.lumix.user.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 已鎖定、尚未完成 email 驗證的註冊申請。
 *
 * <p>passwordHash 已是 BCrypt 結果；此型別不得承載原始密碼或任何原始驗證碼。</p>
 */
public record PendingRegistration(
    UUID registrationId,
    AuthenticatedUser user,
    String passwordHash,
    String numericCodeDigest,
    String letterCodeDigest,
    int attemptCount,
    Instant expiresAt
) {
    public PendingRegistration {
        Objects.requireNonNull(registrationId, "registrationId must not be null");
        Objects.requireNonNull(user, "user must not be null");
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        Objects.requireNonNull(numericCodeDigest, "numericCodeDigest must not be null");
        Objects.requireNonNull(letterCodeDigest, "letterCodeDigest must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
