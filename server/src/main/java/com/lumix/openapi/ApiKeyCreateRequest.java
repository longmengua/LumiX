package com.lumix.openapi;

import com.lumix.account.UserId;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * API key 建立請求模型。
 */
public record ApiKeyCreateRequest(
        UserId userId,
        String name,
        Set<ApiKeyPermission> permissions,
        ApiRateLimitTier rateLimitTier,
        List<String> ipWhitelist
) {

    public ApiKeyCreateRequest {
        Objects.requireNonNull(userId, "userId must not be null");
        name = requireText(name, "name");
        Objects.requireNonNull(permissions, "permissions must not be null");
        permissions = Set.copyOf(permissions);
        Objects.requireNonNull(rateLimitTier, "rateLimitTier must not be null");
        Objects.requireNonNull(ipWhitelist, "ipWhitelist must not be null");
        ipWhitelist = List.copyOf(ipWhitelist);
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
