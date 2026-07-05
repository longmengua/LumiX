package com.lumix.openapi;

/**
 * Open API key 權限。
 */
public enum ApiKeyPermission {
    READ,
    SPOT_TRADE,
    FUTURES_TRADE,
    MARGIN_TRADE,
    // 提現權限預設不可開，後續若要放行必須經過額外人工審查。
    WITHDRAW,
    MARKET_MAKER,
    INTERNAL_MM
}
