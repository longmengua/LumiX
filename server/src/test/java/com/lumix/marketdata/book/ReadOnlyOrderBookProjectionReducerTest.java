package com.lumix.marketdata.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.BookDeltaPayload;
import com.lumix.marketdata.contract.BookLevel;
import com.lumix.marketdata.contract.BookSnapshotPayload;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.InstrumentId;
import com.lumix.marketdata.contract.InstrumentPrecision;
import com.lumix.marketdata.contract.MarketDataChannel;
import com.lumix.marketdata.contract.MarketDataContractViolation;
import com.lumix.marketdata.contract.MarketDataPayload;
import com.lumix.marketdata.contract.MarketDataRejectionReason;
import com.lumix.marketdata.contract.MarketDataSource;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.contract.SchemaVersion;
import com.lumix.marketdata.contract.Sequence;
import com.lumix.marketdata.policy.MarketDataAdmissionResult;
import com.lumix.marketdata.policy.MarketDataStalePolicy;
import com.lumix.marketdata.policy.MarketDataStreamAdmissionPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 T04 reducer 只能將 T03 接納的連續 book event 化為不可變 projection，並在不足資料時 fail closed。
 */
class ReadOnlyOrderBookProjectionReducerTest {

    private static final InstrumentPrecision PRECISION = new InstrumentPrecision(2, 8, 20);
    private static final Instant RECEIVED_TIME = Instant.parse("2026-08-20T00:00:01Z");
    private static final MarketDataStreamAdmissionPolicy ADMISSION = new MarketDataStreamAdmissionPolicy(
            new MarketDataStalePolicy(Duration.ofSeconds(5))
    );
    private final ReadOnlyOrderBookProjectionReducer reducer = new ReadOnlyOrderBookProjectionReducer();

    /**
     * snapshot 建立排序 baseline，連續 delta 可更新／移除價位，但不可改變 immutable 舊 projection。
     */
    @Test
    void appliesSnapshotThenContinuousDeltaWithStablePriceOrdering() {
        NormalizedMarketDataEvent snapshotEvent = event(snapshot(), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult snapshotAdmission = ADMISSION.evaluate(
                snapshotEvent, Optional.empty(), RECEIVED_TIME.plusSeconds(1)
        );
        OrderBookProjectionResult snapshot = reducer.reduce(Optional.empty(), snapshotEvent, snapshotAdmission);

        NormalizedMarketDataEvent deltaEvent = event(delta(), 11, "BTC-USDT", RECEIVED_TIME.plusSeconds(1));
        MarketDataAdmissionResult deltaAdmission = ADMISSION.evaluate(
                deltaEvent, Optional.of(snapshotAdmission.nextCursor()), RECEIVED_TIME.plusSeconds(2)
        );
        OrderBookProjectionResult delta = reducer.reduce(Optional.of(snapshot.projection()), deltaEvent, deltaAdmission);

        assertEquals(OrderBookProjectionDecision.SNAPSHOT_APPLIED, snapshot.decision());
        assertEquals(List.of("100.25", "100.20"), prices(snapshot.projection().bids()));
        assertEquals(List.of("100.30", "100.35"), prices(snapshot.projection().asks()));
        assertEquals(OrderBookProjectionDecision.DELTA_APPLIED, delta.decision());
        assertEquals(List.of("100.25"), prices(delta.projection().bids()));
        assertEquals(List.of("100.30", "100.32"), prices(delta.projection().asks()));
        assertEquals(OrderBookStatus.HEALTHY, delta.projection().status());
        assertEquals(10, snapshot.projection().asOfSequence().orElseThrow().value());
        assertEquals(11, delta.projection().asOfSequence().orElseThrow().value());
    }

    /**
     * 沒有 snapshot 的 delta 即使交給 reducer 也不能形成 book，並必須對 consumer 顯示需要 resync。
     */
    @Test
    void rejectsInitialDeltaWithoutInventingBookBaseline() {
        NormalizedMarketDataEvent deltaEvent = event(delta(), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult admission = ADMISSION.evaluate(deltaEvent, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult result = reducer.reduce(Optional.empty(), deltaEvent, admission);

        assertEquals(OrderBookProjectionDecision.REJECTED, result.decision());
        assertEquals(OrderBookProjectionReason.ADMISSION_NOT_ACCEPTED, result.reason());
        assertEquals(OrderBookStatus.RESYNC_REQUIRED, result.projection().status());
        assertTrue(result.projection().asOfSequence().isEmpty());
        assertFalse(result.applied());
    }

    /**
     * 同一 event 的 duplicate admission 不得二次套用，卻仍可將已過期的 book 降為 STALE。
     */
    @Test
    void ignoresDuplicateWithoutMutatingLevelsButPropagatesStaleStatus() {
        NormalizedMarketDataEvent snapshotEvent = event(snapshot(), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult firstAdmission = ADMISSION.evaluate(snapshotEvent, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult first = reducer.reduce(Optional.empty(), snapshotEvent, firstAdmission);
        MarketDataAdmissionResult duplicateAdmission = ADMISSION.evaluate(
                snapshotEvent, Optional.of(firstAdmission.nextCursor()), RECEIVED_TIME.plusSeconds(5)
        );
        OrderBookProjectionResult duplicate = reducer.reduce(
                Optional.of(first.projection()), snapshotEvent, duplicateAdmission
        );

        assertEquals(OrderBookProjectionDecision.DUPLICATE_IGNORED, duplicate.decision());
        assertEquals(first.projection().bids(), duplicate.projection().bids());
        assertEquals(first.projection().asks(), duplicate.projection().asks());
        assertEquals(OrderBookStatus.STALE, duplicate.projection().status());
        assertFalse(duplicate.applied());
    }

    /**
     * gap event 不能改寫既有 levels，並必須讓 projection 降為 GAP_DETECTED，禁止 consumer 當成健康 book。
     */
    @Test
    void preservesLevelsAndDegradesProjectionWhenAdmissionDetectsGap() {
        NormalizedMarketDataEvent snapshotEvent = event(snapshot(), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult snapshotAdmission = ADMISSION.evaluate(snapshotEvent, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult snapshot = reducer.reduce(Optional.empty(), snapshotEvent, snapshotAdmission);
        NormalizedMarketDataEvent gapEvent = event(delta(), 12, "BTC-USDT", RECEIVED_TIME.plusSeconds(1));
        MarketDataAdmissionResult gapAdmission = ADMISSION.evaluate(
                gapEvent, Optional.of(snapshotAdmission.nextCursor()), RECEIVED_TIME.plusSeconds(1)
        );
        OrderBookProjectionResult gap = reducer.reduce(Optional.of(snapshot.projection()), gapEvent, gapAdmission);

        assertEquals(OrderBookProjectionDecision.REJECTED, gap.decision());
        assertEquals(OrderBookStatus.GAP_DETECTED, gap.projection().status());
        assertEquals(snapshot.projection().bids(), gap.projection().bids());
        assertFalse(gap.applied());
    }

    /**
     * crossed snapshot 不能變成 HEALTHY，也不能覆蓋先前無資料 projection，避免製造虛假的可交易流動性。
     */
    @Test
    void rejectsCrossedSnapshotAsDegradedInsteadOfAuthoritativeBook() {
        NormalizedMarketDataEvent crossedEvent = event(crossedSnapshot(), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult admission = ADMISSION.evaluate(crossedEvent, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult result = reducer.reduce(Optional.empty(), crossedEvent, admission);

        assertEquals(OrderBookProjectionDecision.REJECTED, result.decision());
        assertEquals(OrderBookProjectionReason.CROSSED_BOOK_REJECTED, result.reason());
        assertEquals(OrderBookStatus.DEGRADED, result.projection().status());
        assertTrue(result.projection().bids().isEmpty());
        assertTrue(result.projection().asks().isEmpty());
    }

    /**
     * 合法空 snapshot 仍是一個明確 baseline；空與 unavailable 由 as-of metadata、status 和 isEmpty 三者區分。
     */
    @Test
    void preservesEmptySnapshotAsAnExplicitHealthyBook() {
        NormalizedMarketDataEvent emptyEvent = event(new BookSnapshotPayload(List.of(), List.of()), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult admission = ADMISSION.evaluate(emptyEvent, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult result = reducer.reduce(Optional.empty(), emptyEvent, admission);

        assertEquals(OrderBookProjectionDecision.SNAPSHOT_APPLIED, result.decision());
        assertEquals(OrderBookStatus.HEALTHY, result.projection().status());
        assertTrue(result.projection().isEmpty());
        assertTrue(result.projection().asOfSequence().isPresent());
    }

    /**
     * snapshot 同價位聚合後若超過 T02 precision boundary，必須拒絕而不是截斷、rounding 或建立錯誤數量。
     */
    @Test
    void rejectsSnapshotAggregationOverflowWithoutRounding() {
        String maximum = "99999999999999999999";
        BookSnapshotPayload overflowing = new BookSnapshotPayload(
                List.of(level("100.25", maximum), level("100.25", maximum)),
                List.of(level("100.30", "1"))
        );
        NormalizedMarketDataEvent event = event(overflowing, 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult admission = ADMISSION.evaluate(event, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult result = reducer.reduce(Optional.empty(), event, admission);

        assertEquals(OrderBookProjectionDecision.REJECTED, result.decision());
        assertEquals(OrderBookProjectionReason.QUANTITY_OVERFLOW, result.reason());
        assertEquals(OrderBookStatus.DEGRADED, result.projection().status());
        assertTrue(result.projection().isEmpty());
    }

    /** 公開 projection 建構子也必須守住 reducer 的 fail-closed 不變式，不能自行偽造健康 book。 */
    @Test
    void rejectsDirectProjectionConstructionThatViolatesBookInvariants() {
        NormalizedMarketDataEvent baseline = event(snapshot(), 10, "BTC-USDT", RECEIVED_TIME);

        assertThrows(IllegalArgumentException.class, () -> new ReadOnlyOrderBookProjection(
                baseline.streamKey(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(), OrderBookStatus.HEALTHY
        ));
        assertThrows(IllegalArgumentException.class, () -> new ReadOnlyOrderBookProjection(
                baseline.streamKey(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(), OrderBookStatus.STALE
        ));
        assertThrows(IllegalArgumentException.class, () -> directProjection(
                baseline, List.of(level("100.20", "1"), level("100.25", "1")), List.of(), OrderBookStatus.STALE
        ));
        assertThrows(IllegalArgumentException.class, () -> directProjection(
                baseline, List.of(), List.of(level("100.30", "1"), level("100.30", "1")), OrderBookStatus.STALE
        ));
        assertThrows(IllegalArgumentException.class, () -> directProjection(
                baseline, List.of(level("100.20", "0")), List.of(), OrderBookStatus.STALE
        ));
        assertThrows(IllegalArgumentException.class, () -> directProjection(
                baseline, List.of(level("100.30", "1")), List.of(level("100.30", "1")), OrderBookStatus.HEALTHY
        ));
        List<BookLevel> tooManyDescendingBids = new ArrayList<>(levels(1_025, 1_000));
        Collections.reverse(tooManyDescendingBids);
        assertThrows(IllegalArgumentException.class, () -> directProjection(
                baseline, tooManyDescendingBids, List.of(), OrderBookStatus.STALE
        ));
    }

    /** payload 每側恰好 1,024 筆可受理；第 1,025 筆在 contract 邊界即以固定 reason 拒絕。 */
    @Test
    void acceptsFixedPayloadLevelBoundaryAndRejectsSnapshotAndDeltaOverLimit() {
        List<BookLevel> maximumLevels = levels(1_024, 1_000);
        assertEquals(1_024, new BookSnapshotPayload(maximumLevels, maximumLevels).bids().size());
        assertEquals(1_024, new BookDeltaPayload(maximumLevels, maximumLevels).bidUpdates().size());

        MarketDataContractViolation snapshotViolation = assertThrows(MarketDataContractViolation.class,
                () -> new BookSnapshotPayload(levels(1_025, 1_000), List.of()));
        MarketDataContractViolation deltaViolation = assertThrows(MarketDataContractViolation.class,
                () -> new BookDeltaPayload(levels(1_025, 1_000), List.of()));

        assertEquals(MarketDataRejectionReason.BOOK_LEVEL_LIMIT_EXCEEDED, snapshotViolation.reason());
        assertEquals(MarketDataRejectionReason.BOOK_LEVEL_LIMIT_EXCEEDED, deltaViolation.reason());
    }

    /** delta 雖未超過輸入上限，套用後若使 projection 超限仍須完整拒絕，不能部分保留更新。 */
    @Test
    void rejectsDeltaWhenAppliedProjectionWouldExceedFixedLevelLimit() {
        BookSnapshotPayload maximumSnapshot = new BookSnapshotPayload(levels(1_024, 1_000), levels(1_024, 5_000));
        NormalizedMarketDataEvent snapshotEvent = event(maximumSnapshot, 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult snapshotAdmission = ADMISSION.evaluate(snapshotEvent, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult baseline = reducer.reduce(Optional.empty(), snapshotEvent, snapshotAdmission);
        NormalizedMarketDataEvent deltaEvent = event(
                new BookDeltaPayload(List.of(level("2500.00", "1")), List.of()),
                11,
                "BTC-USDT",
                RECEIVED_TIME.plusSeconds(1)
        );
        MarketDataAdmissionResult deltaAdmission = ADMISSION.evaluate(
                deltaEvent, Optional.of(snapshotAdmission.nextCursor()), RECEIVED_TIME.plusSeconds(1)
        );

        OrderBookProjectionResult result = reducer.reduce(Optional.of(baseline.projection()), deltaEvent, deltaAdmission);

        assertEquals(OrderBookProjectionDecision.REJECTED, result.decision());
        assertEquals(OrderBookProjectionReason.LEVEL_LIMIT_EXCEEDED, result.reason());
        assertEquals(OrderBookStatus.DEGRADED, result.projection().status());
        assertEquals(baseline.projection().bids(), result.projection().bids());
        assertEquals(baseline.projection().asOfSequence(), result.projection().asOfSequence());
    }

    /** 亂序 delta 不得因 T03 保留舊 cursor 而讓既有 projection 繼續顯示為健康。 */
    @Test
    void outOfOrderDeltaPreservesMetadataAndRequiresResync() {
        OrderBookProjectionResult snapshot = acceptedSnapshot(10, "BTC-USDT");
        NormalizedMarketDataEvent outOfOrder = event(delta(), 9, "BTC-USDT", RECEIVED_TIME.plusSeconds(1));
        MarketDataAdmissionResult admission = ADMISSION.evaluate(
                outOfOrder,
                Optional.of(new MarketDataStreamAdmissionPolicy(new MarketDataStalePolicy(Duration.ofSeconds(5)))
                        .evaluate(event(snapshot(), 10, "BTC-USDT", RECEIVED_TIME), Optional.empty(), RECEIVED_TIME).nextCursor()),
                RECEIVED_TIME.plusSeconds(1)
        );
        OrderBookProjectionResult result = reducer.reduce(Optional.of(snapshot.projection()), outOfOrder, admission);

        assertEquals(OrderBookProjectionDecision.REJECTED, result.decision());
        assertEquals(OrderBookStatus.RESYNC_REQUIRED, result.projection().status());
        assertEquals(snapshot.projection().asOfSequence(), result.projection().asOfSequence());
        assertEquals(snapshot.projection().lastAppliedIdentity(), result.projection().lastAppliedIdentity());
        assertEquals(snapshot.projection().bids(), result.projection().bids());
    }

    /** gap 後舊 book 只能停在降級狀態；T03 接受的 resync snapshot 才能建立新 baseline。 */
    @Test
    void resyncSnapshotReplacesGapDegradedBookWithNewBaseline() {
        NormalizedMarketDataEvent first = event(snapshot(), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult firstAdmission = ADMISSION.evaluate(first, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult baseline = reducer.reduce(Optional.empty(), first, firstAdmission);
        NormalizedMarketDataEvent gap = event(delta(), 12, "BTC-USDT", RECEIVED_TIME.plusSeconds(1));
        MarketDataAdmissionResult gapAdmission = ADMISSION.evaluate(gap, Optional.of(firstAdmission.nextCursor()), RECEIVED_TIME.plusSeconds(1));
        OrderBookProjectionResult degraded = reducer.reduce(Optional.of(baseline.projection()), gap, gapAdmission);
        NormalizedMarketDataEvent resync = event(snapshot(), 50, "BTC-USDT", RECEIVED_TIME.plusSeconds(2));
        MarketDataAdmissionResult resyncAdmission = ADMISSION.evaluate(resync, Optional.of(gapAdmission.nextCursor()), RECEIVED_TIME.plusSeconds(2));
        OrderBookProjectionResult recovered = reducer.reduce(Optional.of(degraded.projection()), resync, resyncAdmission);

        assertEquals(OrderBookStatus.GAP_DETECTED, degraded.projection().status());
        assertEquals(OrderBookProjectionDecision.SNAPSHOT_APPLIED, recovered.decision());
        assertEquals(OrderBookStatus.HEALTHY, recovered.projection().status());
        assertEquals(50, recovered.projection().asOfSequence().orElseThrow().value());
    }

    /** BTC projection 不能接收 ETH stream，即使兩者 sequence 相同也不得覆蓋 BTC metadata。 */
    @Test
    void rejectsOtherInstrumentWithoutChangingProjection() {
        OrderBookProjectionResult btc = acceptedSnapshot(10, "BTC-USDT");
        NormalizedMarketDataEvent eth = event(snapshot(), 10, "ETH-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult ethAdmission = ADMISSION.evaluate(eth, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult result = reducer.reduce(Optional.of(btc.projection()), eth, ethAdmission);

        assertEquals(OrderBookProjectionReason.STREAM_KEY_MISMATCH, result.reason());
        assertEquals(btc.projection(), result.projection());
    }

    /** accepted admission 必須是目前 event 本身，不能拿同 stream 的另一筆 accepted cursor 來餵 reducer。 */
    @Test
    void rejectsAdmissionForAnotherEvent() {
        OrderBookProjectionResult snapshot = acceptedSnapshot(10, "BTC-USDT");
        NormalizedMarketDataEvent acceptedDelta = event(delta(), 11, "BTC-USDT", RECEIVED_TIME.plusSeconds(1));
        MarketDataAdmissionResult acceptedAdmission = admissionAfterSnapshot(acceptedDelta);
        NormalizedMarketDataEvent otherEvent = event(delta(), 12, "BTC-USDT", RECEIVED_TIME.plusSeconds(2));
        OrderBookProjectionResult result = reducer.reduce(Optional.of(snapshot.projection()), otherEvent, acceptedAdmission);

        assertEquals(OrderBookProjectionReason.ADMISSION_EVENT_MISMATCH, result.reason());
        assertEquals(snapshot.projection(), result.projection());
    }

    /** sequence 21 對其自身 cursor 雖連續，但不能跨越目前 projection 的 sequence 10 baseline。 */
    @Test
    void rejectsAcceptedAdmissionFromDifferentProjectionBaseline() {
        OrderBookProjectionResult projectionAtTen = acceptedSnapshot(10, "BTC-USDT");
        MarketDataStreamAdmissionPolicy independent = new MarketDataStreamAdmissionPolicy(new MarketDataStalePolicy(Duration.ofSeconds(5)));
        NormalizedMarketDataEvent snapshotAtTwenty = event(snapshot(), 20, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult admissionAtTwenty = independent.evaluate(snapshotAtTwenty, Optional.empty(), RECEIVED_TIME);
        NormalizedMarketDataEvent deltaAtTwentyOne = event(delta(), 21, "BTC-USDT", RECEIVED_TIME.plusSeconds(1));
        MarketDataAdmissionResult admissionAtTwentyOne = independent.evaluate(deltaAtTwentyOne, Optional.of(admissionAtTwenty.nextCursor()), RECEIVED_TIME.plusSeconds(1));
        OrderBookProjectionResult result = reducer.reduce(Optional.of(projectionAtTen.projection()), deltaAtTwentyOne, admissionAtTwentyOne);

        assertEquals(OrderBookProjectionReason.DELTA_SEQUENCE_NOT_CONTINUOUS, result.reason());
        assertEquals(OrderBookStatus.RESYNC_REQUIRED, result.projection().status());
        assertEquals(projectionAtTen.projection().asOfSequence(), result.projection().asOfSequence());
    }

    /** 相同初始狀態與 event/admission 序列必須得到完全相同的 immutable projection。 */
    @Test
    void replaysSameSnapshotAndDeltaDeterministically() {
        OrderBookProjectionResult first = replaySnapshotThenDelta();
        OrderBookProjectionResult second = replaySnapshotThenDelta();

        assertEquals(first, second);
    }

    private OrderBookProjectionResult acceptedSnapshot(long sequence, String instrument) {
        NormalizedMarketDataEvent event = event(snapshot(), sequence, instrument, RECEIVED_TIME);
        MarketDataAdmissionResult admission = ADMISSION.evaluate(event, Optional.empty(), RECEIVED_TIME);
        return reducer.reduce(Optional.empty(), event, admission);
    }

    private MarketDataAdmissionResult admissionAfterSnapshot(NormalizedMarketDataEvent delta) {
        MarketDataStreamAdmissionPolicy policy = new MarketDataStreamAdmissionPolicy(new MarketDataStalePolicy(Duration.ofSeconds(5)));
        NormalizedMarketDataEvent baseline = event(snapshot(), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult baselineAdmission = policy.evaluate(baseline, Optional.empty(), RECEIVED_TIME);
        return policy.evaluate(delta, Optional.of(baselineAdmission.nextCursor()), delta.receivedTimestamp());
    }

    private OrderBookProjectionResult replaySnapshotThenDelta() {
        MarketDataStreamAdmissionPolicy policy = new MarketDataStreamAdmissionPolicy(new MarketDataStalePolicy(Duration.ofSeconds(5)));
        NormalizedMarketDataEvent snapshot = event(snapshot(), 10, "BTC-USDT", RECEIVED_TIME);
        MarketDataAdmissionResult snapshotAdmission = policy.evaluate(snapshot, Optional.empty(), RECEIVED_TIME);
        OrderBookProjectionResult baseline = reducer.reduce(Optional.empty(), snapshot, snapshotAdmission);
        NormalizedMarketDataEvent delta = event(delta(), 11, "BTC-USDT", RECEIVED_TIME.plusSeconds(1));
        MarketDataAdmissionResult deltaAdmission = policy.evaluate(delta, Optional.of(snapshotAdmission.nextCursor()), RECEIVED_TIME.plusSeconds(1));
        return reducer.reduce(Optional.of(baseline.projection()), delta, deltaAdmission);
    }

    private static NormalizedMarketDataEvent event(
            MarketDataPayload payload,
            long sequence,
            String instrument,
            Instant receivedTimestamp
    ) {
        return new NormalizedMarketDataEvent(
                new MarketDataSource("fixture-source"),
                MarketDataChannel.BOOK,
                new InstrumentId(instrument),
                payload.eventType(),
                new Sequence(sequence),
                Instant.parse("2026-08-20T00:00:00Z").plusSeconds(sequence),
                receivedTimestamp,
                SchemaVersion.V1,
                PRECISION,
                payload
        );
    }

    private static BookSnapshotPayload snapshot() {
        return new BookSnapshotPayload(
                List.of(level("100.20", "100000000"), level("100.25", "200000000")),
                List.of(level("100.35", "200000000"), level("100.30", "100000000"))
        );
    }

    private static BookSnapshotPayload crossedSnapshot() {
        return new BookSnapshotPayload(List.of(level("100.30", "100000000")), List.of(level("100.30", "100000000")));
    }

    private static BookDeltaPayload delta() {
        return new BookDeltaPayload(
                List.of(level("100.20", "0")),
                List.of(level("100.35", "0"), level("100.32", "300000000"))
        );
    }

    private static BookLevel level(String price, String quantity) {
        return new BookLevel(DecimalPrice.fromWire(price, PRECISION), AtomicQuantity.fromWire(quantity, PRECISION));
    }

    private static ReadOnlyOrderBookProjection directProjection(
            NormalizedMarketDataEvent baseline,
            List<BookLevel> bids,
            List<BookLevel> asks,
            OrderBookStatus status
    ) {
        return new ReadOnlyOrderBookProjection(
                baseline.streamKey(),
                Optional.of(baseline.sequence()),
                Optional.of(baseline.sourceTimestamp()),
                Optional.of(baseline.identity()),
                bids,
                asks,
                status
        );
    }

    private static List<BookLevel> levels(int count, int firstWholePrice) {
        List<BookLevel> levels = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            levels.add(level((firstWholePrice + index) + ".00", "1"));
        }
        return List.copyOf(levels);
    }

    private static List<String> prices(List<BookLevel> levels) {
        return levels.stream().map(level -> level.price().toWireString()).toList();
    }
}
