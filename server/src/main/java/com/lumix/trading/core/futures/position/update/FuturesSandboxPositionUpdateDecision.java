package com.lumix.trading.core.futures.position.update;

/**
 * Futures sandbox position update gate 的決策結果。
 *
 * OPENED_FOR_SANDBOX 只回傳記憶體內 immutable snapshots，不代表 position 已持久化或保證金已保留。
 */
public enum FuturesSandboxPositionUpdateDecision {
    OPENED_FOR_SANDBOX,
    REJECTED
}
