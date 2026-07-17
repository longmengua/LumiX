package com.lumix.trading.core.futures.matching;

/**
 * Futures sandbox matching reuse gate 的可審計結果原因。
 *
 * 這些 reason 只描述 immutable candidate evaluation，不能被當成交易撮合或成交 API 的狀態碼。
 */
public enum FuturesSandboxMatchingReason {
    CROSSED_LIMIT_PRICE,
    NO_CROSSED_ORDERS,
    MARKET_MISMATCH,
    DUPLICATE_ORDER_ID
}
