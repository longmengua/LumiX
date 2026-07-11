package com.lumix.trading.core.spot.orderintake;

/**
 * sandbox order 的方向。
 *
 * 這只代表 BUY / SELL 的 intake 語意，不代表正式 matching 或 settlement 已完成。
 */
public enum SpotOrderSide {
    BUY,
    SELL
}
