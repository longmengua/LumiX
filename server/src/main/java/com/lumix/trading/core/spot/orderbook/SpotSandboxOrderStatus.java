package com.lumix.trading.core.spot.orderbook;

/**
 * sandbox order 的狀態。
 *
 * 這些狀態只代表 sandbox in-memory book 的生命週期，不代表正式 matching / settlement 已完成。
 */
public enum SpotSandboxOrderStatus {
    ACCEPTED,
    OPEN,
    REJECTED,
    CANCELLED,
    FILLED,
    PARTIALLY_FILLED
}
