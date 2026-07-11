package com.lumix.trading.core.spot.orderbook;

/**
 * Spot sandbox order book 的拒絕原因。
 *
 * 這些 code 只服務 in-memory book 的測試與維護，不應被誤解為正式 production API contract。
 */
public enum SpotSandboxOrderBookRejectionReason {
    INTAKE_REJECTED,
    DUPLICATE_IDEMPOTENCY_KEY,
    UNSUPPORTED_ORDER_TYPE,
    UNSUPPORTED_TIME_IN_FORCE
}
