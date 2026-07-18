package com.lumix.trading.core.futures.sandbox.funding;

/**
 * Funding preview 對 position 的資金方向。
 *
 * PAY / RECEIVE 只描述預覽中的經濟方向，絕不代表已執行扣款、入帳或 settlement。
 */
public enum FuturesSandboxFundingDirection {
    PAY,
    RECEIVE,
    NONE
}
