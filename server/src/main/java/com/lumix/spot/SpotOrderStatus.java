package com.lumix.spot;

/**
 * 現貨訂單狀態。
 */
public enum SpotOrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED,
    REJECTED,
    EXPIRED
}
