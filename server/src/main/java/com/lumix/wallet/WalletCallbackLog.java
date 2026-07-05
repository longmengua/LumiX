package com.lumix.wallet;

import java.time.Instant;
import java.util.Objects;

/**
 * 錢包 callback / retry 紀錄模型。
 */
public record WalletCallbackLog(
        String id,
        String callbackType,
        String referenceId,
        int attemptCount,
        String payloadDigest,
        Instant createdAt,
        Instant lastAttemptAt
) {

    public WalletCallbackLog {
        id = requireText(id, "id");
        callbackType = requireText(callbackType, "callbackType");
        referenceId = requireText(referenceId, "referenceId");
        payloadDigest = normalizeOptionalText(payloadDigest);
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(lastAttemptAt, "lastAttemptAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
