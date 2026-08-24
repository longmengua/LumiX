package com.lumix.marketdata.replay;

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
import org.junit.jupiter.api.Test;

class DeterministicMarketDataReplayerTest {

    private static final InstrumentPrecision PRECISION = new InstrumentPrecision(2, 8, 20);
    private static final Instant BASE_TIME = Instant.parse("2026-08-01T00:00:00Z");
    private final DeterministicMarketDataReplayer replayer = new DeterministicMarketDataReplayer(Duration.ofMinutes(5));

    @Test
    void canonicalizedInputOrderProducesTheSameTraceStateAndDigest() {
        // 不變式：caller list 的排列不可改變 replay 結果，否則 audit 重放沒有可信比較基礎。
        NormalizedMarketDataEvent first = trade("BTC-USDT", 1, "100.00");
        NormalizedMarketDataEvent second = trade("BTC-USDT", 2, "101.00");
        MarketDataReplayResult forward = replay(List.of(first, second));
        MarketDataReplayResult reversed = replay(List.of(second, first));

        assertFalse(forward.failed());
        assertEquals(forward.finalState(), reversed.finalState());
        assertEquals(forward.trace(), reversed.trace());
        assertEquals(forward.digest(), reversed.digest());
    }

    @Test
    void gapCreatesOneResyncRequestAndCompatibleSnapshotClearsIt() {
        // gap 不能被略過；只有同一 book stream 的後續 snapshot 才能消除 pending resync。
        NormalizedMarketDataEvent snapshot = bookSnapshot("BTC-USDT", 1, "100.00", "100.01");
        NormalizedMarketDataEvent gap = bookDelta("BTC-USDT", 3, "100.00");
        MarketDataReplayResult degraded = replay(List.of(snapshot, gap));
        assertTrue(degraded.finalState().pendingResyncRequests().containsKey(snapshot.streamKey()));
        assertEquals(ResyncReason.ADMISSION_GAP_OR_RESYNC, degraded.finalState().pendingResyncRequests().get(snapshot.streamKey()).reason());

        NormalizedMarketDataEvent recovery = bookSnapshot("BTC-USDT", 4, "101.00", "101.01");
        MarketDataReplayResult recovered = replay(List.of(snapshot, gap, recovery));
        assertFalse(recovered.finalState().pendingResyncRequests().containsKey(snapshot.streamKey()));
        assertEquals(4, recovered.finalState().books().get(snapshot.streamKey()).asOfSequence().orElseThrow().value());
    }

    @Test
    void unhealthyStreamDoesNotBlockIndependentInstrumentReplay() {
        // per-stream isolation：BTC gap 的 recovery 不可讓 ETH trade aggregation 停止或共用 sequence。
        NormalizedMarketDataEvent btcSnapshot = bookSnapshot("BTC-USDT", 1, "100.00", "100.01");
        NormalizedMarketDataEvent btcGap = bookDelta("BTC-USDT", 4, "100.00");
        NormalizedMarketDataEvent ethTrade = trade("ETH-USDT", 1, "200.00");

        MarketDataReplayResult result = replay(List.of(btcGap, ethTrade, btcSnapshot));

        assertTrue(result.finalState().pendingResyncRequests().containsKey(btcSnapshot.streamKey()));
        assertTrue(result.finalState().aggregations().get(ethTrade.streamKey()).ticker().isPresent());
        assertEquals("200.00", result.finalState().aggregations().get(ethTrade.streamKey()).ticker().orElseThrow().last().toWireString());
    }

    @Test
    void distinctIdentitiesAtTheSameStreamSequenceAreRejectedAsAmbiguous() {
        // 同 stream/same sequence 的不同 payload 沒有合法 canonical 順序，不能以 list 順序偷偷決定 winner。
        NormalizedMarketDataEvent first = trade("BTC-USDT", 1, "100.00");
        NormalizedMarketDataEvent conflict = trade("BTC-USDT", 1, "101.00");

        MarketDataReplayResult result = replay(List.of(first, conflict));

        assertTrue(result.failed());
        assertEquals(ReplayFailureReason.AMBIGUOUS_CANONICAL_ORDER, result.failureReason().orElseThrow());
        assertTrue(result.trace().isEmpty());
        assertEquals(MarketDataReplayState.empty(), result.finalState());
    }

    private MarketDataReplayResult replay(List<NormalizedMarketDataEvent> events) {
        return replayer.replay(new MarketDataReplayInput(MarketDataReplayState.empty(), events, BASE_TIME.plusSeconds(30)));
    }

    private static NormalizedMarketDataEvent trade(String instrument, long sequence, String price) {
        return event(
                instrument, MarketDataChannel.TRADES, MarketDataEventType.TRADE, sequence,
                new TradePayload("trade-" + instrument + '-' + sequence + '-' + price, value(price), quantity("100000000"))
        );
    }

    private static NormalizedMarketDataEvent bookSnapshot(String instrument, long sequence, String bid, String ask) {
        return event(
                instrument, MarketDataChannel.BOOK, MarketDataEventType.BOOK_SNAPSHOT, sequence,
                new BookSnapshotPayload(List.of(new BookLevel(value(bid), quantity("100000000"))), List.of(new BookLevel(value(ask), quantity("100000000"))))
        );
    }

    private static NormalizedMarketDataEvent bookDelta(String instrument, long sequence, String bid) {
        return event(
                instrument, MarketDataChannel.BOOK, MarketDataEventType.BOOK_DELTA, sequence,
                new BookDeltaPayload(List.of(new BookLevel(value(bid), quantity("200000000"))), List.of())
        );
    }

    private static NormalizedMarketDataEvent event(
            String instrument,
            MarketDataChannel channel,
            MarketDataEventType type,
            long sequence,
            MarketDataPayload payload
    ) {
        Instant time = BASE_TIME.plusSeconds(sequence);
        return new NormalizedMarketDataEvent(
                new MarketDataSource("fixture-source"), channel, new InstrumentId(instrument), type, new Sequence(sequence), time,
                time.plusSeconds(1), SchemaVersion.V1, PRECISION, payload
        );
    }

    private static DecimalPrice value(String value) {
        return DecimalPrice.fromWire(value, PRECISION);
    }

    private static AtomicQuantity quantity(String value) {
        return AtomicQuantity.fromWire(value, PRECISION);
    }
}
