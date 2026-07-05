package com.lumix.openapi;

/**
 * API 限流等級。
 */
public enum ApiRateLimitTier {
    RETAIL,
    VIP,
    MARKET_MAKER,
    INTERNAL_MM,
    ADMIN
}
