package com.lumix.openapi;

/**
 * Open API 限流契約。
 */
public interface ApiRateLimitService {

    // TODO(HUMAN_REVIEW_REQUIRED): 回傳此 principal / route / tier 是否允許通行。
    boolean isAllowed(String principalId, ApiRateLimitTier tier, OpenApiRoute route);
}
