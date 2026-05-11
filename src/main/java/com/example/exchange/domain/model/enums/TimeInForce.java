package com.example.exchange.domain.model.enums;

/**
 * TimeInForce 表示訂單在市場中的有效時間規則
 *
 * 常見於交易所、撮合系統、股票或加密貨幣交易。
 */
public enum TimeInForce {

    /**
     * Good Till Cancelled
     * 訂單會一直保留在訂單簿中，直到：
     * 1. 完全成交
     * 2. 使用者取消
     */
    GTC,

    /**
     * Immediate Or Cancel
     * 訂單送出後必須立即撮合：
     * - 能成交的部分立即成交
     * - 剩餘未成交部分立刻取消
     */
    IOC,

    /**
     * Fill Or Kill
     * 訂單送出後必須「全部立即成交」：
     * - 若無法全部成交
     * - 整張訂單直接取消，不會部分成交
     */
    FOK
}