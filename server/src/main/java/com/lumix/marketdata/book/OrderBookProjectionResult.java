package com.lumix.marketdata.book;

import java.util.Objects;

/**
 * 一次 reducer transition 的 immutable 結果；只有 applied decision 代表 input event 改變了 levels。
 */
public record OrderBookProjectionResult(
        OrderBookProjectionDecision decision,
        OrderBookProjectionReason reason,
        ReadOnlyOrderBookProjection projection
) {

    public OrderBookProjectionResult {
        decision = Objects.requireNonNull(decision, "decision must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null");
        projection = Objects.requireNonNull(projection, "projection must not be null");
    }

    /**
     * projection consumer 只能在這兩種結果下觀察到新的 levels；仍須另外檢查 status 是否為 HEALTHY。
     */
    public boolean applied() {
        return decision == OrderBookProjectionDecision.SNAPSHOT_APPLIED
                || decision == OrderBookProjectionDecision.DELTA_APPLIED;
    }
}
