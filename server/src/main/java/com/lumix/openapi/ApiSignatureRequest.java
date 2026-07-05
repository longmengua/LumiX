package com.lumix.openapi;

import java.time.Instant;
import java.util.Objects;

/**
 * 簽名驗證輸入模型。
 */
public record ApiSignatureRequest(
        String apiKeyId,
        Instant timestamp,
        String httpMethod,
        String path,
        String canonicalQuery,
        String body,
        String signature,
        String sourceIp
) {

    public ApiSignatureRequest {
        apiKeyId = requireText(apiKeyId, "apiKeyId");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        httpMethod = requireText(httpMethod, "httpMethod");
        path = requireText(path, "path");
        canonicalQuery = normalizeOptionalText(canonicalQuery);
        body = normalizeOptionalText(body);
        signature = requireText(signature, "signature");
        sourceIp = requireText(sourceIp, "sourceIp");
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
