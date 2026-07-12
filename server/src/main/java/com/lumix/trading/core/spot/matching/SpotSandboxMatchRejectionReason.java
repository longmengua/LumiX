package com.lumix.trading.core.spot.matching;

/**
 * Spot sandbox matching runtime 的拒絕原因。
 *
 * 這些 code 只服務 sandbox matching 的測試與維護，不應被誤解為正式 production API contract。
 */
public enum SpotSandboxMatchRejectionReason {
    MARKET_NOT_FOUND,
    NO_CROSSED_ORDERS,
    UNSUPPORTED_ORDER_TYPE,
    UNSUPPORTED_TIME_IN_FORCE,
    MISSING_RESERVATION_BOUNDARY,
    HUMAN_REVIEW_REQUIRED
}
