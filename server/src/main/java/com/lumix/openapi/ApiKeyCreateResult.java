package com.lumix.openapi;

import java.util.Objects;

/**
 * API key 建立結果。
 * secret 只在建立時回傳一次。
 */
public record ApiKeyCreateResult(
        ApiKeyView apiKey,
        String plainTextSecret,
        boolean secretShownOnce
) {

    public ApiKeyCreateResult {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        plainTextSecret = requireText(plainTextSecret, "plainTextSecret");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
