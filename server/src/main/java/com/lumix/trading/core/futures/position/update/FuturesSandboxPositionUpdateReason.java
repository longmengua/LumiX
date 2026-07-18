package com.lumix.trading.core.futures.position.update;

/**
 * Futures sandbox one-way、open-only position update 的結果原因。
 */
public enum FuturesSandboxPositionUpdateReason {
    OPENED_FROM_VERIFIED_FILL,
    FILL_ALREADY_PROCESSED,
    BUYER_POSITION_SCOPE_MISMATCH,
    SELLER_POSITION_SCOPE_MISMATCH,
    BUYER_POSITION_ALREADY_OPEN,
    SELLER_POSITION_ALREADY_OPEN
}
