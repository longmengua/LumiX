package com.lumix.marketdata.replay;

import com.lumix.marketdata.aggregation.AggregationStatus;
import com.lumix.marketdata.aggregation.ReadOnlyTradeTickerCandleReducer;
import com.lumix.marketdata.aggregation.TradeAggregationResult;
import com.lumix.marketdata.book.OrderBookStatus;
import com.lumix.marketdata.book.ReadOnlyOrderBookProjectionReducer;
import com.lumix.marketdata.book.OrderBookProjectionResult;
import com.lumix.marketdata.contract.MarketDataEventType;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.contract.StreamKey;
import com.lumix.marketdata.policy.FeedHealth;
import com.lumix.marketdata.policy.MarketDataAdmissionResult;
import com.lumix.marketdata.policy.MarketDataStalePolicy;
import com.lumix.marketdata.policy.MarketDataStreamAdmissionPolicy;
import com.lumix.marketdata.policy.MarketDataStreamCursor;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * P21-T06 的 pure replay coordinator。
 *
 * <p>它刻意建立在 T03/T04/T05 reducer 之上，所有 map 都只存在於本次 method call；因此任何 resync 都只輸出
 * request contract，絕不 reconnect、fetch snapshot、sleep、讀 clock 或持久化 offset。</p>
 */
public final class DeterministicMarketDataReplayer {

    private final MarketDataStreamAdmissionPolicy admissionPolicy;
    private final ReadOnlyOrderBookProjectionReducer bookReducer;
    private final ReadOnlyTradeTickerCandleReducer tradeReducer;

    /**
     * 建構使用固定 stale policy 的 coordinator；evaluation timestamp 仍由每個 replay input 明確指定。
     */
    public DeterministicMarketDataReplayer(Duration maximumReceivedAge) {
        this.admissionPolicy = new MarketDataStreamAdmissionPolicy(new MarketDataStalePolicy(maximumReceivedAge));
        this.bookReducer = new ReadOnlyOrderBookProjectionReducer();
        this.tradeReducer = new ReadOnlyTradeTickerCandleReducer();
    }

    /**
     * 以 stream key/sequence/identity canonicalize 後重放。相同序列號若有不同 identity 即拒絕，不能依 caller list 順序猜測。
     */
    public MarketDataReplayResult replay(MarketDataReplayInput input) {
        input = Objects.requireNonNull(input, "input must not be null");
        List<NormalizedMarketDataEvent> events = canonicalize(input.events());
        Optional<ReplayFailureReason> ambiguity = ambiguousOrder(events);
        if (ambiguity.isPresent()) {
            return result(input.initialState(), List.of(), ambiguity);
        }

        Map<StreamKey, MarketDataStreamCursor> cursors = new HashMap<>(input.initialState().cursors());
        Map<StreamKey, com.lumix.marketdata.book.ReadOnlyOrderBookProjection> books = new HashMap<>(input.initialState().books());
        Map<StreamKey, com.lumix.marketdata.aggregation.TradeAggregationProjection> aggregations = new HashMap<>(input.initialState().aggregations());
        Map<StreamKey, ResyncRequest> resyncs = new HashMap<>(input.initialState().pendingResyncRequests());
        List<ReplayTransitionTrace> trace = new ArrayList<>();

        for (int index = 0; index < events.size(); index++) {
            NormalizedMarketDataEvent event = events.get(index);
            if (!cursors.containsKey(event.streamKey()) && cursors.size() >= MarketDataReplayState.MAX_STREAMS) {
                MarketDataReplayState state = state(cursors, books, aggregations, resyncs);
                return result(state, trace, Optional.of(ReplayFailureReason.STREAM_LIMIT_EXCEEDED));
            }
            MarketDataAdmissionResult admission = admissionPolicy.evaluate(
                    event, Optional.ofNullable(cursors.get(event.streamKey())), input.evaluationTimestamp()
            );
            cursors.put(event.streamKey(), admission.nextCursor());
            TransitionOutcome outcome = applyProjection(event, admission, books, aggregations);
            updateResync(event, admission.nextCursor().health(), outcome, resyncs);
            trace.add(new ReplayTransitionTrace(
                    index, event.identity(), admission.decision(), admission.reason(), outcome.decision(),
                    admission.nextCursor().health(), resyncs.containsKey(event.streamKey())
            ));
        }
        return result(state(cursors, books, aggregations, resyncs), trace, Optional.empty());
    }

    private TransitionOutcome applyProjection(
            NormalizedMarketDataEvent event,
            MarketDataAdmissionResult admission,
            Map<StreamKey, com.lumix.marketdata.book.ReadOnlyOrderBookProjection> books,
            Map<StreamKey, com.lumix.marketdata.aggregation.TradeAggregationProjection> aggregations
    ) {
        if (event.eventType() == MarketDataEventType.BOOK_SNAPSHOT || event.eventType() == MarketDataEventType.BOOK_DELTA) {
            OrderBookProjectionResult result = bookReducer.reduce(Optional.ofNullable(books.get(event.streamKey())), event, admission);
            books.put(event.streamKey(), result.projection());
            return new TransitionOutcome(result.decision().name(), result.projection().status() == OrderBookStatus.HEALTHY);
        }
        if (event.eventType() == MarketDataEventType.TRADE) {
            TradeAggregationResult result = tradeReducer.reduce(Optional.ofNullable(aggregations.get(event.streamKey())), event, admission);
            aggregations.put(event.streamKey(), result.projection());
            return new TransitionOutcome(result.decision().name(), result.projection().status() == AggregationStatus.HEALTHY);
        }
        // T05 尚未將 TICKER payload 視為 aggregation input；replay 仍保留 T03 cursor trace，但不虛構 projection。
        return new TransitionOutcome("NO_PROJECTION_CONSUMER", admission.shouldApplyEvent());
    }

    private static void updateResync(
            NormalizedMarketDataEvent event,
            FeedHealth health,
            TransitionOutcome outcome,
            Map<StreamKey, ResyncRequest> resyncs
    ) {
        if (health == FeedHealth.HEALTHY && outcome.healthy()) {
            // 相容 snapshot 恢復後才移除 request；任何其他 healthy admission 不能替不完整 projection 假裝 recovery。
            if (event.eventType() == MarketDataEventType.BOOK_SNAPSHOT) {
                resyncs.remove(event.streamKey());
            }
            return;
        }
        ResyncReason reason = health == FeedHealth.GAP_DETECTED || health == FeedHealth.RESYNC_REQUIRED || health == FeedHealth.DEGRADED
                ? ResyncReason.ADMISSION_GAP_OR_RESYNC
                : ResyncReason.PROJECTION_REJECTED_OR_DEGRADED;
        resyncs.putIfAbsent(event.streamKey(), new ResyncRequest(event.streamKey(), reason, event.sourceTimestamp()));
    }

    private static List<NormalizedMarketDataEvent> canonicalize(List<NormalizedMarketDataEvent> events) {
        List<NormalizedMarketDataEvent> sorted = new ArrayList<>(events);
        sorted.sort(Comparator
                .comparing((NormalizedMarketDataEvent event) -> streamToken(event.streamKey()))
                .thenComparingLong(event -> event.sequence().value())
                .thenComparing(event -> event.identity().payloadFingerprint()));
        return List.copyOf(sorted);
    }

    private static Optional<ReplayFailureReason> ambiguousOrder(List<NormalizedMarketDataEvent> events) {
        for (int index = 1; index < events.size(); index++) {
            NormalizedMarketDataEvent previous = events.get(index - 1);
            NormalizedMarketDataEvent current = events.get(index);
            if (previous.streamKey().equals(current.streamKey())
                    && previous.sequence().equals(current.sequence())
                    && !previous.identity().equals(current.identity())) {
                return Optional.of(ReplayFailureReason.AMBIGUOUS_CANONICAL_ORDER);
            }
        }
        return Optional.empty();
    }

    private static MarketDataReplayState state(
            Map<StreamKey, MarketDataStreamCursor> cursors,
            Map<StreamKey, com.lumix.marketdata.book.ReadOnlyOrderBookProjection> books,
            Map<StreamKey, com.lumix.marketdata.aggregation.TradeAggregationProjection> aggregations,
            Map<StreamKey, ResyncRequest> resyncs
    ) {
        return new MarketDataReplayState(cursors, books, aggregations, resyncs);
    }

    private static MarketDataReplayResult result(
            MarketDataReplayState state,
            List<ReplayTransitionTrace> trace,
            Optional<ReplayFailureReason> failure
    ) {
        return new MarketDataReplayResult(state, trace, digest(state, trace, failure), failure);
    }

    private static ReplayDigest digest(
            MarketDataReplayState state,
            List<ReplayTransitionTrace> trace,
            Optional<ReplayFailureReason> failure
    ) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder canonical = new StringBuilder();
            state.cursors().entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(DeterministicMarketDataReplayer::streamToken)))
                    .forEach(entry -> canonical.append("C|").append(streamToken(entry.getKey())).append('|')
                            .append(entry.getValue().health()).append('|')
                            .append(entry.getValue().lastAcceptedEvent().map(value -> value.sequence().value() + ":" + value.identity().payloadFingerprint()).orElse("-")).append('\n'));
            state.books().entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(DeterministicMarketDataReplayer::streamToken)))
                    .forEach(entry -> canonical.append("B|").append(streamToken(entry.getKey())).append('|').append(entry.getValue().status()).append('|')
                            .append(entry.getValue().asOfSequence().map(value -> Long.toString(value.value())).orElse("-")).append('|')
                            .append(entry.getValue().bids().stream().map(level -> level.price().toWireString() + ":" + level.quantity().toWireString()).toList()).append('|')
                            .append(entry.getValue().asks().stream().map(level -> level.price().toWireString() + ":" + level.quantity().toWireString()).toList()).append('\n'));
            state.aggregations().entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(DeterministicMarketDataReplayer::streamToken)))
                    .forEach(entry -> canonical.append("A|").append(streamToken(entry.getKey())).append('|').append(entry.getValue().status()).append('|')
                            .append(entry.getValue().asOfSequence().map(value -> Long.toString(value.value())).orElse("-")).append('|')
                            .append(entry.getValue().ticker().map(value -> value.last().toWireString() + ":" + value.baseVolume().toWireString() + ":" + value.quoteVolume().atoms()).orElse("-")).append('\n'));
            state.pendingResyncRequests().entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(DeterministicMarketDataReplayer::streamToken)))
                    .forEach(entry -> canonical.append("R|").append(streamToken(entry.getKey())).append('|').append(entry.getValue().reason()).append('|')
                            .append(entry.getValue().detectedAtSourceTimestamp()).append('\n'));
            trace.forEach(value -> canonical.append("T|").append(value.canonicalIndex()).append('|').append(value.eventIdentity().payloadFingerprint())
                    .append('|').append(value.admissionDecision()).append('|').append(value.projectionDecision()).append('|').append(value.resyncPending()).append('\n'));
            canonical.append("F|").append(failure.map(Enum::name).orElse("-")).append('\n');
            return new ReplayDigest(HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8))));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the Java runtime", exception);
        }
    }

    private static String streamToken(StreamKey key) {
        return key.source().value() + '|' + key.channel().name() + '|' + key.instrumentId().value();
    }

    private record TransitionOutcome(String decision, boolean healthy) {
    }
}
