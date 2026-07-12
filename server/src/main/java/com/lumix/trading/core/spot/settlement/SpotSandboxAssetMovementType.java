package com.lumix.trading.core.spot.settlement;

/**
 * Spot sandbox settlement 的資產移動型別。
 *
 * 這些型別只用來描述 sandbox settlement plan，不能被誤解為正式資金流已執行。
 */
public enum SpotSandboxAssetMovementType {
    /**
     * 收到 base asset。
     */
    RECEIVE_BASE,

    /**
     * 支付 quote asset。
     */
    PAY_QUOTE,

    /**
     * 支付 base asset。
     */
    PAY_BASE,

    /**
     * 收到 quote asset。
     */
    RECEIVE_QUOTE
}
