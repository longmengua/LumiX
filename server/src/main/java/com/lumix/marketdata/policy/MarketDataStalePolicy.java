package com.lumix.marketdata.policy;

import java.time.Duration;
import java.util.Objects;

/**
 * 以注入門檻判定行情是否 stale 的純設定。
 *
 * <p>policy 不讀取系統 clock；呼叫端必須提供 evaluation timestamp，才能讓同一批事件在
 * replay 時得到相同 health transition。</p>
 */
public record MarketDataStalePolicy(Duration maximumReceivedAge) {

    public MarketDataStalePolicy {
        maximumReceivedAge = Objects.requireNonNull(maximumReceivedAge, "maximumReceivedAge must not be null");
        if (maximumReceivedAge.isNegative() || maximumReceivedAge.isZero()) {
            throw new IllegalArgumentException("maximumReceivedAge must be positive");
        }
    }
}
