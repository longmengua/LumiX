package com.lumix.marketdata.query;

import java.util.Objects;

/** 每個 subscriber 自有的固定 pending capacity 與 overflow policy，不讓慢 consumer 阻塞 reducer。 */
public record SubscriptionPolicy(int maximumPendingUpdates, BackpressureStrategy backpressureStrategy) {

    public SubscriptionPolicy {
        backpressureStrategy = Objects.requireNonNull(backpressureStrategy, "backpressureStrategy must not be null");
        if (maximumPendingUpdates < 1 || maximumPendingUpdates > 1_024) {
            throw new IllegalArgumentException("subscriber pending capacity must be between 1 and 1024");
        }
    }
}
