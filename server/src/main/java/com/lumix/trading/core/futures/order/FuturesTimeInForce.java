package com.lumix.trading.core.futures.order;

/**
 * Futures sandbox time-in-force。
 *
 * T01 刻意只支援 GTC，避免在尚未進入 order book / matching runtime 前提前建立 IOC、FOK 或 POST_ONLY 的假能力。
 */
public enum FuturesTimeInForce {
    GTC
}
