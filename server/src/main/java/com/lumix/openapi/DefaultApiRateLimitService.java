package com.lumix.openapi;

import java.util.Objects;

/**
 * Phase 10 限流 stub。
 * 採用 fail-closed 行為，不接 Redis 或任何真實配額系統。
 */
public class DefaultApiRateLimitService implements ApiRateLimitService {

    @Override
    public boolean isAllowed(String principalId, ApiRateLimitTier tier, OpenApiRoute route) {
        Objects.requireNonNull(principalId, "principalId must not be null");
        if (principalId.trim().isEmpty()) {
            throw new IllegalArgumentException("principalId must not be blank");
        }
        Objects.requireNonNull(tier, "tier must not be null");
        Objects.requireNonNull(route, "route must not be null");

        // TODO: requires high-reasoning review before production use
        // Placeholder only. This stub does not implement counters, bursts, or distributed enforcement.
        return false;
    }
}
