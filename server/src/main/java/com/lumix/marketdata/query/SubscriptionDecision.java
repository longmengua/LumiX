package com.lumix.marketdata.query;

import java.util.Objects;
import java.util.Optional;

/** pure policy result；沒有 queue 寫入、network send 或 subscriber registry mutation。 */
public record SubscriptionDecision(
        SubscriptionOutcome outcome,
        SubscriptionReason reason,
        Optional<SubscriberCursor> nextCursor,
        Optional<MarketDataViewEnvelope> envelope
) {

    public SubscriptionDecision {
        outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        reason = Objects.requireNonNull(reason, "reason must not be null");
        nextCursor = Objects.requireNonNull(nextCursor, "nextCursor must not be null");
        envelope = Objects.requireNonNull(envelope, "envelope must not be null");
    }
}
