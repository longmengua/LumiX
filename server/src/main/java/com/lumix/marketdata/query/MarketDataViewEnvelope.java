package com.lumix.marketdata.query;

import com.lumix.marketdata.contract.StreamKey;
import java.time.Instant;
import java.util.Objects;

/**
 * internal query/stream 的固定 envelope。所有 consumer 都必須看到 projection version、來源 as-of 時間與 health。
 */
public record MarketDataViewEnvelope(
        StreamKey streamKey,
        long projectionVersion,
        Instant asOfSourceTimestamp,
        String health,
        MarketDataReadOnlyView view
) {

    public MarketDataViewEnvelope {
        streamKey = Objects.requireNonNull(streamKey, "streamKey must not be null");
        asOfSourceTimestamp = Objects.requireNonNull(asOfSourceTimestamp, "asOfSourceTimestamp must not be null");
        health = Objects.requireNonNull(health, "health must not be null");
        view = Objects.requireNonNull(view, "view must not be null");
        if (projectionVersion < 1
                || !streamKey.equals(view.streamKey())
                || projectionVersion != view.asOfSequence()
                || !asOfSourceTimestamp.equals(view.asOfSourceTimestamp())
                || !health.equals(view.health())) {
            throw new IllegalArgumentException("envelope metadata must exactly match immutable projection view");
        }
    }

    /** health 字串僅有 HEALTHY 時能以連續 update 發送；其餘狀態必須讓 consumer resnapshot。 */
    public boolean isHealthy() {
        return "HEALTHY".equals(health);
    }

    public static MarketDataViewEnvelope from(MarketDataReadOnlyView view) {
        return new MarketDataViewEnvelope(view.streamKey(), view.asOfSequence(), view.asOfSourceTimestamp(), view.health(), view);
    }
}
