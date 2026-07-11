package com.lumix.trading.core.spot.orderintake;

/**
 * Spot sandbox order intake 的拒絕原因。
 *
 * 這些 code 只服務 sandbox boundary 的測試與維護，不應被誤解為正式 production API contract。
 */
public enum SpotSandboxOrderRejectionReason {
    MISSING_REQUEST_ID,
    MISSING_IDEMPOTENCY_KEY,
    MISSING_USER_ID,
    MISSING_ACCOUNT_ID,
    MISSING_MARKET_SYMBOL,
    MISSING_SIDE,
    MISSING_ORDER_TYPE,
    MISSING_TIME_IN_FORCE,
    UNSUPPORTED_ORDER_TYPE,
    UNSUPPORTED_TIME_IN_FORCE,
    INVALID_PRICE,
    INVALID_QUANTITY
}
