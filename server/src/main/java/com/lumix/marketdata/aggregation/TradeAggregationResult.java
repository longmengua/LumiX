package com.lumix.marketdata.aggregation;

import java.util.Objects;

/**
 * 單筆 trade reducer transition 的 immutable 回傳值；只有 {@link #applied()} 為 true 時才有新的 OHLCV/ticker。
 */
public record TradeAggregationResult(
        TradeAggregationDecision decision,
        TradeAggregationReason reason,
        TradeAggregationProjection projection
) {

    public TradeAggregationResult {
        decision = Objects.requireNonNull(decision, "decision must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null");
        projection = Objects.requireNonNull(projection, "projection must not be null");
    }

    /**
     * 僅 APPLIED 代表 input event 改變了可觀察的聚合資料；consumer 仍必須檢查 projection status。
     */
    public boolean applied() {
        return decision == TradeAggregationDecision.APPLIED;
    }
}
