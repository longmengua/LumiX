package com.lumix.trading.core.spot.orderbook;

/**
 * Spot sandbox order book 的決策結果。
 *
 * 這個 decision 只表示 sandbox book 是否接受 record，不代表任何後續交易流程已完成。
 */
public enum SpotSandboxOrderBookDecision {
    ACCEPTED,
    REJECTED,
    DUPLICATE
}
