package com.lumix.trading.core.spot.matching;

/**
 * Spot sandbox 目前允許或預留的 matching 設計能力。
 *
 * 這些能力只代表 sandbox 路線上的設計邊界，不代表任何正式撮合 runtime 已完成。
 */
public enum SpotSandboxMatchingCapability {
    MARKET_PARTITION,
    PRICE_PRIORITY,
    TIME_PRIORITY,
    CROSSED_LIMIT_PRICE,
    PARTIAL_FILL_SEMANTICS,
    SETTLEMENT_INPUT_BOUNDARY
}
