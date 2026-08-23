package com.lumix.marketdata.aggregation;

import java.time.Instant;
import java.util.Objects;

/**
 * candle 的來源時間半開窗口；end 不包含在此窗口，避免相鄰 candle 對邊界 trade 重複計入。
 */
public record CandleWindow(Instant startInclusive, Instant endExclusive) {

    public CandleWindow {
        startInclusive = Objects.requireNonNull(startInclusive, "startInclusive must not be null");
        endExclusive = Objects.requireNonNull(endExclusive, "endExclusive must not be null");
        if (!endExclusive.isAfter(startInclusive)) {
            throw new IllegalArgumentException("candle window end must be after start");
        }
    }

    /**
     * 檢查來源事件時間是否屬於此 bucket；此防線避免 caller 以錯誤 window 偽造 OHLCV。
     */
    public boolean contains(Instant timestamp) {
        timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        return !timestamp.isBefore(startInclusive) && timestamp.isBefore(endExclusive);
    }
}
