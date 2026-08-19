package com.lumix.marketdata.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.BookDeltaPayload;
import com.lumix.marketdata.contract.BookLevel;
import com.lumix.marketdata.contract.BookSnapshotPayload;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.InstrumentId;
import com.lumix.marketdata.contract.InstrumentPrecision;
import com.lumix.marketdata.contract.MarketDataChannel;
import com.lumix.marketdata.contract.MarketDataEventType;
import com.lumix.marketdata.contract.MarketDataPayload;
import com.lumix.marketdata.contract.MarketDataSource;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.contract.SchemaVersion;
import com.lumix.marketdata.contract.Sequence;
import com.lumix.marketdata.contract.TradePayload;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 驗證 P21-T03 不變式：同 stream 只能連續套用、異常必須降級且所有 decision 可固定重放。
 */
class MarketDataStreamAdmissionPolicyTest {

    private static final InstrumentPrecision PRECISION = new InstrumentPrecision(2, 8, 20);
    private static final Instant SOURCE_TIME = Instant.parse("2026-08-19T00:00:00Z");
    private static final Instant RECEIVED_TIME = Instant.parse("2026-08-19T00:00:01Z");
    private static final MarketDataStreamAdmissionPolicy POLICY = new MarketDataStreamAdmissionPolicy(
            new MarketDataStalePolicy(Duration.ofSeconds(5))
    );

    /**
     * snapshot 是 book stream 的唯一 baseline；連續 delta 才能被套用，重送同 identity 必須是 no-op。
     */
    @Test
    void acceptsSnapshotAndContiguousDeltaButIgnoresDuplicate() {
        MarketDataAdmissionResult snapshot = POLICY.evaluate(
                event(bookSnapshot(), 10, "BTC-USDT", RECEIVED_TIME),
                Optional.empty(),
                RECEIVED_TIME.plusSeconds(1)
        );
        MarketDataAdmissionResult delta = POLICY.evaluate(
                event(bookDelta("100.26"), 11, "BTC-USDT", RECEIVED_TIME.plusSeconds(1)),
                Optional.of(snapshot.nextCursor()),
                RECEIVED_TIME.plusSeconds(2)
        );
        MarketDataAdmissionResult duplicate = POLICY.evaluate(
                event(bookDelta("100.26"), 11, "BTC-USDT", RECEIVED_TIME.plusSeconds(1)),
                Optional.of(delta.nextCursor()),
                RECEIVED_TIME.plusSeconds(2)
        );

        assertEquals(MarketDataAdmissionDecision.ACCEPTED, snapshot.decision());
        assertEquals(MarketDataAdmissionDecision.ACCEPTED, delta.decision());
        assertEquals(FeedHealth.HEALTHY, delta.nextCursor().health());
        assertEquals(MarketDataAdmissionDecision.DUPLICATE_IGNORED, duplicate.decision());
        assertFalse(duplicate.shouldApplyEvent());
        assertEquals(delta.nextCursor(), duplicate.nextCursor());
    }

    /**
     * delta 沒有可驗證的初始 book state，不能把它當作 baseline；cursor 也不得記成該 delta 已被接受。
     */
    @Test
    void requiresResyncForInitialBookDeltaWithoutInventingAcceptedCursor() {
        MarketDataAdmissionResult result = POLICY.evaluate(
                event(bookDelta("100.26"), 10, "BTC-USDT", RECEIVED_TIME),
                Optional.empty(),
                RECEIVED_TIME
        );

        assertEquals(MarketDataAdmissionDecision.RESYNC_REQUIRED, result.decision());
        assertEquals(MarketDataAdmissionReason.INITIAL_BOOK_DELTA_REQUIRES_SNAPSHOT, result.reason());
        assertEquals(FeedHealth.RESYNC_REQUIRED, result.nextCursor().health());
        assertTrue(result.nextCursor().lastAcceptedEvent().isEmpty());
        assertFalse(result.shouldApplyEvent());
    }

    /**
     * 同 sequence 但不同 payload 不能猜測哪筆為真；必須進入 DEGRADED，且只有 snapshot 可重建 baseline。
     */
    @Test
    void rejectsConflictingPayloadAndRequiresSnapshotRecovery() {
        MarketDataAdmissionResult initial = POLICY.evaluate(
                event(bookSnapshot(), 10, "BTC-USDT", RECEIVED_TIME), Optional.empty(), RECEIVED_TIME
        );
        MarketDataAdmissionResult conflict = POLICY.evaluate(
                event(bookSnapshot("100.27"), 10, "BTC-USDT", RECEIVED_TIME),
                Optional.of(initial.nextCursor()),
                RECEIVED_TIME
        );
        MarketDataAdmissionResult blockedDelta = POLICY.evaluate(
                event(bookDelta("100.28"), 11, "BTC-USDT", RECEIVED_TIME.plusSeconds(1)),
                Optional.of(conflict.nextCursor()),
                RECEIVED_TIME.plusSeconds(1)
        );
        MarketDataAdmissionResult recovery = POLICY.evaluate(
                event(bookSnapshot("100.28"), 50, "BTC-USDT", RECEIVED_TIME.plusSeconds(2)),
                Optional.of(blockedDelta.nextCursor()),
                RECEIVED_TIME.plusSeconds(2)
        );

        assertEquals(MarketDataAdmissionDecision.INTEGRITY_CONFLICT, conflict.decision());
        assertEquals(FeedHealth.DEGRADED, conflict.nextCursor().health());
        assertEquals(MarketDataAdmissionDecision.RESYNC_REQUIRED, blockedDelta.decision());
        assertEquals(FeedHealth.RESYNC_REQUIRED, blockedDelta.nextCursor().health());
        assertEquals(MarketDataAdmissionDecision.ACCEPTED, recovery.decision());
        assertEquals(MarketDataAdmissionReason.RESYNC_SNAPSHOT_ACCEPTED, recovery.reason());
        assertEquals(FeedHealth.HEALTHY, recovery.nextCursor().health());
    }

    /**
     * gap 當下必須留下 GAP_DETECTED，下一筆非 snapshot 轉為 RESYNC_REQUIRED，兩者都不可套用。
     */
    @Test
    void detectsGapAndNeverAdmitsDeltaUntilSnapshotRecovery() {
        MarketDataAdmissionResult snapshot = POLICY.evaluate(
                event(bookSnapshot(), 10, "BTC-USDT", RECEIVED_TIME), Optional.empty(), RECEIVED_TIME
        );
        MarketDataAdmissionResult gap = POLICY.evaluate(
                event(bookDelta("100.26"), 12, "BTC-USDT", RECEIVED_TIME.plusSeconds(1)),
                Optional.of(snapshot.nextCursor()),
                RECEIVED_TIME.plusSeconds(1)
        );
        MarketDataAdmissionResult pendingResync = POLICY.evaluate(
                event(bookDelta("100.27"), 13, "BTC-USDT", RECEIVED_TIME.plusSeconds(2)),
                Optional.of(gap.nextCursor()),
                RECEIVED_TIME.plusSeconds(2)
        );

        assertEquals(MarketDataAdmissionDecision.GAP_DETECTED, gap.decision());
        assertEquals(FeedHealth.GAP_DETECTED, gap.nextCursor().health());
        assertFalse(gap.shouldApplyEvent());
        assertEquals(MarketDataAdmissionDecision.RESYNC_REQUIRED, pendingResync.decision());
        assertEquals(FeedHealth.RESYNC_REQUIRED, pendingResync.nextCursor().health());
        assertFalse(pendingResync.shouldApplyEvent());
    }

    /**
     * 比最後 accepted sequence 更小的事件永遠不得回填 cursor，避免亂序資料覆蓋可重放順序。
     */
    @Test
    void rejectsOutOfOrderEventWithoutChangingAcceptedSequence() {
        MarketDataAdmissionResult baseline = POLICY.evaluate(
                event(trade("trade-10", "100.25"), 10, "BTC-USDT", RECEIVED_TIME), Optional.empty(), RECEIVED_TIME
        );
        MarketDataAdmissionResult accepted = POLICY.evaluate(
                event(trade("trade-11", "100.26"), 11, "BTC-USDT", RECEIVED_TIME.plusSeconds(1)),
                Optional.of(baseline.nextCursor()), RECEIVED_TIME.plusSeconds(1)
        );
        MarketDataAdmissionResult outOfOrder = POLICY.evaluate(
                event(trade("trade-09", "100.24"), 9, "BTC-USDT", RECEIVED_TIME.plusSeconds(2)),
                Optional.of(accepted.nextCursor()), RECEIVED_TIME.plusSeconds(2)
        );

        assertEquals(MarketDataAdmissionDecision.OUT_OF_ORDER_REJECTED, outOfOrder.decision());
        assertEquals(MarketDataAdmissionReason.OUT_OF_ORDER_SEQUENCE, outOfOrder.reason());
        assertEquals(11, outOfOrder.nextCursor().requireLastAcceptedEvent().sequence().value());
        assertFalse(outOfOrder.shouldApplyEvent());
    }

    /**
     * stale 只依 received time 與注入 evaluation time 判定；相同輸入重放時結果不得依機器 clock 改變。
     */
    @Test
    void marksOldReceivedEventStaleDeterministically() {
        NormalizedMarketDataEvent oldTrade = event(trade("trade-10", "100.25"), 10, "BTC-USDT", RECEIVED_TIME);

        MarketDataAdmissionResult firstReplay = POLICY.evaluate(
                oldTrade, Optional.empty(), RECEIVED_TIME.plusSeconds(5)
        );
        MarketDataAdmissionResult secondReplay = POLICY.evaluate(
                oldTrade, Optional.empty(), RECEIVED_TIME.plusSeconds(5)
        );

        assertTrue(firstReplay.shouldApplyEvent());
        assertEquals(MarketDataAdmissionReason.STALE_EVENT_ACCEPTED, firstReplay.reason());
        assertEquals(FeedHealth.STALE, firstReplay.nextCursor().health());
        assertEquals(firstReplay, secondReplay);
    }

    /**
     * stream key 必須完整隔離；BTC 的 cursor 不能拿來判定 ETH，避免不同 instrument 的 sequence 相互污染。
     */
    @Test
    void rejectsCursorFromAnotherInstrument() {
        MarketDataAdmissionResult btc = POLICY.evaluate(
                event(trade("trade-10", "100.25"), 10, "BTC-USDT", RECEIVED_TIME), Optional.empty(), RECEIVED_TIME
        );
        MarketDataAdmissionResult eth = POLICY.evaluate(
                event(trade("trade-10", "100.25"), 10, "ETH-USDT", RECEIVED_TIME),
                Optional.of(btc.nextCursor()), RECEIVED_TIME
        );

        assertEquals(MarketDataAdmissionDecision.STREAM_MISMATCH_REJECTED, eth.decision());
        assertEquals(MarketDataAdmissionReason.STREAM_KEY_MISMATCH, eth.reason());
        assertEquals(btc.nextCursor(), eth.nextCursor());
    }

    /**
     * STOPPED 是顯式 terminal health，防止沒有重啟/恢復流程時重新接納任何資料。
     */
    @Test
    void stoppedStreamRejectsFurtherEvents() {
        MarketDataAdmissionResult accepted = POLICY.evaluate(
                event(trade("trade-10", "100.25"), 10, "BTC-USDT", RECEIVED_TIME), Optional.empty(), RECEIVED_TIME
        );
        MarketDataStreamCursor stopped = POLICY.stop(accepted.nextCursor());
        MarketDataAdmissionResult result = POLICY.evaluate(
                event(trade("trade-11", "100.26"), 11, "BTC-USDT", RECEIVED_TIME.plusSeconds(1)),
                Optional.of(stopped), RECEIVED_TIME.plusSeconds(1)
        );

        assertEquals(MarketDataAdmissionDecision.STOPPED, result.decision());
        assertEquals(FeedHealth.STOPPED, result.nextCursor().health());
        assertFalse(result.shouldApplyEvent());
    }

    private static NormalizedMarketDataEvent event(
            MarketDataPayload payload,
            long sequence,
            String instrument,
            Instant receivedTimestamp
    ) {
        return new NormalizedMarketDataEvent(
                new MarketDataSource("fixture-source"),
                channelFor(payload),
                new InstrumentId(instrument),
                payload.eventType(),
                new Sequence(sequence),
                SOURCE_TIME.plusSeconds(sequence),
                receivedTimestamp,
                SchemaVersion.V1,
                PRECISION,
                payload
        );
    }

    private static MarketDataChannel channelFor(MarketDataPayload payload) {
        return switch (payload.eventType()) {
            case BOOK_SNAPSHOT, BOOK_DELTA -> MarketDataChannel.BOOK;
            case TRADE -> MarketDataChannel.TRADES;
            case TICKER -> MarketDataChannel.TICKER;
        };
    }

    private static BookSnapshotPayload bookSnapshot() {
        return bookSnapshot("100.25");
    }

    private static BookSnapshotPayload bookSnapshot(String bidPrice) {
        return new BookSnapshotPayload(
                List.of(new BookLevel(price(bidPrice), quantity("125000000"))),
                List.of(new BookLevel(price("100.30"), quantity("200000000")))
        );
    }

    private static BookDeltaPayload bookDelta(String bidPrice) {
        return new BookDeltaPayload(List.of(new BookLevel(price(bidPrice), quantity("0"))), List.of());
    }

    private static TradePayload trade(String tradeId, String price) {
        return new TradePayload(tradeId, price(price), quantity("125000000"));
    }

    private static DecimalPrice price(String value) {
        return DecimalPrice.fromWire(value, PRECISION);
    }

    private static AtomicQuantity quantity(String value) {
        return AtomicQuantity.fromWire(value, PRECISION);
    }
}
