package com.lumix.openapi;

import java.util.Objects;
import java.util.Set;

/**
 * Open API route metadata。
 * 只描述路由契約，不建立任何 controller。
 */
public record OpenApiRoute(
        String method,
        String path,
        boolean privateRoute,
        boolean timestampRequired,
        boolean signatureRequired,
        Set<ApiKeyPermission> requiredPermissions,
        ApiRateLimitTier rateLimitTier,
        String description
) {

    public OpenApiRoute {
        method = requireText(method, "method");
        path = requireText(path, "path");
        Objects.requireNonNull(requiredPermissions, "requiredPermissions must not be null");
        requiredPermissions = Set.copyOf(requiredPermissions);
        Objects.requireNonNull(rateLimitTier, "rateLimitTier must not be null");
        description = requireText(description, "description");
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
