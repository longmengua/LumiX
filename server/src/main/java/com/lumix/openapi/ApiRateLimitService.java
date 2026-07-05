package com.lumix.openapi;

/**
 * Open API 限流契約。
 */
public interface ApiRateLimitService {

    // TODO: requires high-reasoning review before production use
    boolean isAllowed(String principalId, ApiRateLimitTier tier, OpenApiRoute route);
}
