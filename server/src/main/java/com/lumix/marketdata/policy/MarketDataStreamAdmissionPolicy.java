package com.lumix.marketdata.policy;

import com.lumix.marketdata.contract.MarketDataEventType;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * P21-T03 的 per-stream、deterministic admission state machine。
 *
 * <p>此類別刻意沒有共享 cursor map、timer 或外部 I/O。呼叫端必須依相同 stream 序列化 transition，
 * 並把前一 immutable cursor 明確傳入；不同 stream 因此不會共用 sequence，也不會因不受控 stream
 * cardinality 讓本 policy 累積記憶體狀態。</p>
 */
public final class MarketDataStreamAdmissionPolicy {

    private final MarketDataStalePolicy stalePolicy;

    public MarketDataStreamAdmissionPolicy(MarketDataStalePolicy stalePolicy) {
        this.stalePolicy = Objects.requireNonNull(stalePolicy, "stalePolicy must not be null");
    }

    /**
     * 對單筆 event 計算下一 cursor 與 decision，不產生任何 side effect。
     *
     * <p>evaluationTimestamp 必須由呼叫端明確提供；stale 僅比較它與 received timestamp，絕不以
     * source timestamp 或本機 wall clock 猜測資料新鮮度。</p>
     */
    public MarketDataAdmissionResult evaluate(
            NormalizedMarketDataEvent event,
            Optional<MarketDataStreamCursor> previousCursor,
            Instant evaluationTimestamp
    ) {
        event = Objects.requireNonNull(event, "event must not be null");
        previousCursor = Objects.requireNonNull(previousCursor, "previousCursor must not be null");
        evaluationTimestamp = Objects.requireNonNull(evaluationTimestamp, "evaluationTimestamp must not be null");

        if (previousCursor.isEmpty()) {
            return admitInitial(event, evaluationTimestamp);
        }

        MarketDataStreamCursor cursor = previousCursor.get();
        if (!cursor.streamKey().equals(event.streamKey())) {
            return result(
                    MarketDataAdmissionDecision.STREAM_MISMATCH_REJECTED,
                    MarketDataAdmissionReason.STREAM_KEY_MISMATCH,
                    refreshStaleness(cursor, evaluationTimestamp)
            );
        }
        if (cursor.health() == FeedHealth.STOPPED) {
            return result(MarketDataAdmissionDecision.STOPPED, MarketDataAdmissionReason.STREAM_STOPPED, cursor);
        }
        if (requiresSnapshotRecovery(cursor.health())) {
            if (event.eventType() == MarketDataEventType.BOOK_SNAPSHOT) {
                return accept(event, FeedHealth.HEALTHY, MarketDataAdmissionReason.RESYNC_SNAPSHOT_ACCEPTED, evaluationTimestamp);
            }
            return result(
                    MarketDataAdmissionDecision.RESYNC_REQUIRED,
                    MarketDataAdmissionReason.RESYNC_PENDING,
                    cursor.withHealth(FeedHealth.RESYNC_REQUIRED)
            );
        }

        return admitComparableEvent(event, cursor, evaluationTimestamp);
    }

    /**
     * 將既有 stream 明確停止。停止後 policy 拒絕所有 event，避免下游把未經重啟流程的資料當成有效 feed。
     */
    public MarketDataStreamCursor stop(MarketDataStreamCursor cursor) {
        return Objects.requireNonNull(cursor, "cursor must not be null").withHealth(FeedHealth.STOPPED);
    }

    private MarketDataAdmissionResult admitInitial(NormalizedMarketDataEvent event, Instant evaluationTimestamp) {
        if (event.eventType() == MarketDataEventType.BOOK_DELTA) {
            return result(
                    MarketDataAdmissionDecision.RESYNC_REQUIRED,
                    MarketDataAdmissionReason.INITIAL_BOOK_DELTA_REQUIRES_SNAPSHOT,
                    MarketDataStreamCursor.awaitingResync(event.streamKey())
            );
        }
        return accept(event, FeedHealth.HEALTHY, MarketDataAdmissionReason.INITIAL_BASELINE_ACCEPTED, evaluationTimestamp);
    }

    private MarketDataAdmissionResult admitComparableEvent(
            NormalizedMarketDataEvent event,
            MarketDataStreamCursor cursor,
            Instant evaluationTimestamp
    ) {
        MarketDataStreamCursor.AcceptedMarketDataEvent lastAcceptedEvent = cursor.requireLastAcceptedEvent();
        long incoming = event.sequence().value();
        long lastAccepted = lastAcceptedEvent.sequence().value();

        if (incoming == lastAccepted) {
            if (lastAcceptedEvent.identity().equals(event.identity())) {
                return result(
                        MarketDataAdmissionDecision.DUPLICATE_IGNORED,
                        MarketDataAdmissionReason.DUPLICATE_IDENTITY,
                        refreshStaleness(cursor, evaluationTimestamp)
                );
            }
            return result(
                    MarketDataAdmissionDecision.INTEGRITY_CONFLICT,
                    MarketDataAdmissionReason.CONFLICTING_PAYLOAD_FOR_SEQUENCE,
                    cursor.withHealth(FeedHealth.DEGRADED)
            );
        }
        if (incoming < lastAccepted || lastAccepted == Long.MAX_VALUE) {
            return result(
                    MarketDataAdmissionDecision.OUT_OF_ORDER_REJECTED,
                    MarketDataAdmissionReason.OUT_OF_ORDER_SEQUENCE,
                    refreshStaleness(cursor, evaluationTimestamp)
            );
        }
        if (incoming > lastAccepted + 1) {
            return result(
                    MarketDataAdmissionDecision.GAP_DETECTED,
                    MarketDataAdmissionReason.SEQUENCE_GAP,
                    cursor.withHealth(FeedHealth.GAP_DETECTED)
            );
        }
        return accept(event, FeedHealth.HEALTHY, MarketDataAdmissionReason.CONTIGUOUS_SEQUENCE_ACCEPTED, evaluationTimestamp);
    }

    private MarketDataAdmissionResult accept(
            NormalizedMarketDataEvent event,
            FeedHealth healthyCandidate,
            MarketDataAdmissionReason healthyReason,
            Instant evaluationTimestamp
    ) {
        MarketDataStreamCursor cursor = cursorFor(event, healthFor(event.receivedTimestamp(), healthyCandidate, evaluationTimestamp));
        MarketDataAdmissionReason reason = cursor.health() == FeedHealth.STALE
                ? MarketDataAdmissionReason.STALE_EVENT_ACCEPTED
                : healthyReason;
        return result(MarketDataAdmissionDecision.ACCEPTED, reason, cursor);
    }

    private MarketDataStreamCursor refreshStaleness(MarketDataStreamCursor cursor, Instant evaluationTimestamp) {
        if (cursor.health() != FeedHealth.HEALTHY && cursor.health() != FeedHealth.STALE) {
            return cursor;
        }
        return cursor.withHealth(healthFor(
                cursor.requireLastAcceptedEvent().receivedTimestamp(),
                cursor.health(),
                evaluationTimestamp
        ));
    }

    private FeedHealth healthFor(Instant receivedTimestamp, FeedHealth healthyCandidate, Instant evaluationTimestamp) {
        Duration receivedAge = Duration.between(receivedTimestamp, evaluationTimestamp);
        if (receivedAge.compareTo(stalePolicy.maximumReceivedAge()) >= 0) {
            return FeedHealth.STALE;
        }
        return healthyCandidate;
    }

    private static boolean requiresSnapshotRecovery(FeedHealth health) {
        return health == FeedHealth.GAP_DETECTED
                || health == FeedHealth.RESYNC_REQUIRED
                || health == FeedHealth.DEGRADED;
    }

    private static MarketDataStreamCursor cursorFor(NormalizedMarketDataEvent event, FeedHealth health) {
        return new MarketDataStreamCursor(
                event.streamKey(),
                Optional.of(new MarketDataStreamCursor.AcceptedMarketDataEvent(
                        event.sequence(),
                        event.sourceTimestamp(),
                        event.receivedTimestamp(),
                        event.identity()
                )),
                health
        );
    }

    private static MarketDataAdmissionResult result(
            MarketDataAdmissionDecision decision,
            MarketDataAdmissionReason reason,
            MarketDataStreamCursor cursor
    ) {
        return new MarketDataAdmissionResult(decision, reason, cursor);
    }
}
