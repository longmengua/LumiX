package com.lumix.openapi;

import com.lumix.account.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * API key 展示模型。
 * 不包含任何明文 secret。
 */
public record ApiKeyView(
        String apiKeyId,
        UserId userId,
        String name,
        Set<ApiKeyPermission> permissions,
        ApiKeyStatus status,
        ApiRateLimitTier rateLimitTier,
        List<String> ipWhitelist,
        Instant createdAt,
        Instant updatedAt
) {

    public ApiKeyView {
        apiKeyId = requireText(apiKeyId, "apiKeyId");
        Objects.requireNonNull(userId, "userId must not be null");
        name = requireText(name, "name");
        Objects.requireNonNull(permissions, "permissions must not be null");
        permissions = Set.copyOf(permissions);
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(rateLimitTier, "rateLimitTier must not be null");
        Objects.requireNonNull(ipWhitelist, "ipWhitelist must not be null");
        ipWhitelist = List.copyOf(ipWhitelist);
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
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
