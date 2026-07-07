package com.lumix.openapi;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Phase 10 簽名驗證 stub。
 * 採用 fail-closed 行為，避免被誤用為可上線安全實作。
 */
public class DefaultApiSignatureVerifier implements ApiSignatureVerifier {

    private final Duration maxClockSkew;

    public DefaultApiSignatureVerifier(Duration maxClockSkew) {
        this.maxClockSkew = Objects.requireNonNull(maxClockSkew, "maxClockSkew must not be null");
    }

    @Override
    public boolean verify(ApiSignatureRequest request, ApiKeyView apiKeyView) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(apiKeyView, "apiKeyView must not be null");

        if (apiKeyView.status() != ApiKeyStatus.ACTIVE) {
            return false;
        }
        if (request.timestamp().isBefore(Instant.now().minus(maxClockSkew))) {
            return false;
        }

        // TODO(HUMAN_REVIEW_REQUIRED): 目前不做 canonical payload 驗證與 secret-based verification，避免把骨架誤認為安全實作。
        return false;
    }
}
