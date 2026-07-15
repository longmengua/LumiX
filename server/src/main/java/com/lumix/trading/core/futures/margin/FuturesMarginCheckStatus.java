package com.lumix.trading.core.futures.margin;

/**
 * Isolated margin sufficiency gate 的最終判斷。
 *
 * 這個 status 只回答 sandbox margin capacity 是否足夠，不代表資金已被保留、order 已被接受或 position 已被建立。
 */
public enum FuturesMarginCheckStatus {
    APPROVED,
    REJECTED
}
