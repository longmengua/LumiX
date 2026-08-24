package com.lumix.marketdata.replay;

import com.lumix.marketdata.contract.MarketDataEventIdentity;
import com.lumix.marketdata.policy.FeedHealth;
import com.lumix.marketdata.policy.MarketDataAdmissionDecision;
import com.lumix.marketdata.policy.MarketDataAdmissionReason;
import java.util.Objects;

/**
 * 單筆 event 的純 trace；僅保留 identity/固定 decision，不保存 provider payload 或任何 credential。
 */
public record ReplayTransitionTrace(
        int canonicalIndex,
        MarketDataEventIdentity eventIdentity,
        MarketDataAdmissionDecision admissionDecision,
        MarketDataAdmissionReason admissionReason,
        String projectionDecision,
        FeedHealth resultingFeedHealth,
        boolean resyncPending
) {

    public ReplayTransitionTrace {
        if (canonicalIndex < 0) {
            throw new IllegalArgumentException("canonicalIndex must not be negative");
        }
        eventIdentity = Objects.requireNonNull(eventIdentity, "eventIdentity must not be null");
        admissionDecision = Objects.requireNonNull(admissionDecision, "admissionDecision must not be null");
        admissionReason = Objects.requireNonNull(admissionReason, "admissionReason must not be null");
        projectionDecision = Objects.requireNonNull(projectionDecision, "projectionDecision must not be null");
        resultingFeedHealth = Objects.requireNonNull(resultingFeedHealth, "resultingFeedHealth must not be null");
    }
}
