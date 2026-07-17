package com.lumix.trading.core.futures.order;

/**
 * Futures sandbox order placement 的最小狀態。
 *
 * 這個 status 只描述 placement gate 是否接受 sandbox order，不代表已進入 order book、已 matched、
 * 已 filled、已 reserve margin、已建立 position 或已 settlement。
 */
public enum FuturesOrderStatus {
    ACCEPTED_FOR_SANDBOX,
    REJECTED
}
