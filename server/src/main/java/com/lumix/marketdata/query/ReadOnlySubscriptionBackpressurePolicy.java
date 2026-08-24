package com.lumix.marketdata.query;

import java.util.Objects;
import java.util.Optional;

/**
 * P21-T07 pure subscriber policy。
 *
 * <p>version gap、non-healthy view 與 capacity overflow 都清除 consumer cursor 或斷線；這比保留 cursor 後
 * 靜默跳過 update 更重要，因為 consumer 否則會把不完整資料誤判為連續行情。</p>
 */
public final class ReadOnlySubscriptionBackpressurePolicy {

    /**
     * 判斷一筆 immutable envelope 是否可送給此 subscriber；呼叫端自行處理實際 queue/network，因此慢 consumer 不會阻塞 reducer。
     */
    public SubscriptionDecision evaluate(
            SubscriberCursor cursor,
            MarketDataViewEnvelope envelope,
            SubscriptionPolicy policy
    ) {
        cursor = Objects.requireNonNull(cursor, "cursor must not be null");
        envelope = Objects.requireNonNull(envelope, "envelope must not be null");
        policy = Objects.requireNonNull(policy, "policy must not be null");
        if (!cursor.streamKey().equals(envelope.streamKey())) {
            return resnapshot(SubscriptionReason.STREAM_KEY_MISMATCH, cursor, envelope, policy);
        }
        if (!envelope.isHealthy()) {
            return resnapshot(SubscriptionReason.NON_HEALTHY_VIEW, cursor, envelope, policy);
        }
        if (cursor.lastDeliveredVersion().isPresent()) {
            long previous = cursor.lastDeliveredVersion().getAsLong();
            if (envelope.projectionVersion() <= previous) {
                return new SubscriptionDecision(
                        SubscriptionOutcome.DUPLICATE_IGNORED, SubscriptionReason.DUPLICATE_OR_OLDER_VERSION,
                        Optional.of(cursor), Optional.empty()
                );
            }
            if (envelope.projectionVersion() != previous + 1) {
                return resnapshot(SubscriptionReason.VERSION_GAP, cursor, envelope, policy);
            }
        }
        if (cursor.pendingUpdates() >= policy.maximumPendingUpdates()) {
            return resnapshot(SubscriptionReason.CONSUMER_CAPACITY_EXCEEDED, cursor, envelope, policy);
        }
        return new SubscriptionDecision(
                SubscriptionOutcome.PUBLISHED, SubscriptionReason.CONTIGUOUS_VERSION,
                Optional.of(new SubscriberCursor(cursor.streamKey(), java.util.OptionalLong.of(envelope.projectionVersion()), cursor.pendingUpdates() + 1)),
                Optional.of(envelope)
        );
    }

    private static SubscriptionDecision resnapshot(
            SubscriptionReason reason,
            SubscriberCursor cursor,
            MarketDataViewEnvelope envelope,
            SubscriptionPolicy policy
    ) {
        if (policy.backpressureStrategy() == BackpressureStrategy.DISCONNECT_AND_RESNAPSHOT) {
            return new SubscriptionDecision(SubscriptionOutcome.DISCONNECTED_AND_RESNAPSHOT, reason, Optional.empty(), Optional.empty());
        }
        return new SubscriptionDecision(
                SubscriptionOutcome.RESNAPSHOT_REQUIRED, reason, Optional.of(SubscriberCursor.fresh(cursor.streamKey())), Optional.empty()
        );
    }
}
