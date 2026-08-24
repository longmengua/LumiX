package com.lumix.marketdata.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.marketdata.book.OrderBookStatus;
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
import com.lumix.marketdata.query.BackpressureStrategy;
import com.lumix.marketdata.query.MarketDataReadOnlyView;
import com.lumix.marketdata.query.MarketDataViewEnvelope;
import com.lumix.marketdata.query.ReadOnlySubscriptionBackpressurePolicy;
import com.lumix.marketdata.query.SubscriberCursor;
import com.lumix.marketdata.query.SubscriptionOutcome;
import com.lumix.marketdata.query.SubscriptionPolicy;
import com.lumix.marketdata.replay.DeterministicMarketDataReplayer;
import com.lumix.marketdata.replay.MarketDataReplayInput;
import com.lumix.marketdata.replay.MarketDataReplayResult;
import com.lumix.marketdata.replay.MarketDataReplayState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketDataFoundationIntegrationTest {

    private static final InstrumentPrecision PRECISION = new InstrumentPrecision(2, 8, 20);
    private static final Instant BASE_TIME = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void snapshotDeltaDuplicateGapRecoveryAndInternalConsumerRemainDeterministic() {
        // 跨 T03-T07：缺號不可靜默略過，snapshot recovery 後才恢復健康，consumer 仍只能讀 immutable envelope。
        NormalizedMarketDataEvent snapshot = bookSnapshot(1, "100.00", "100.01");
        NormalizedMarketDataEvent delta = bookDelta(2, "100.00");
        NormalizedMarketDataEvent gap = bookDelta(4, "100.00");
        NormalizedMarketDataEvent recovery = bookSnapshot(5, "101.00", "101.01");
        NormalizedMarketDataEvent trade = trade(1, "200.00");
        DeterministicMarketDataReplayer replayer = new DeterministicMarketDataReplayer(Duration.ofMinutes(5));

        MarketDataReplayResult first = replay(replayer, List.of(gap, trade, delta, snapshot, delta, recovery));
        MarketDataReplayResult second = replay(replayer, List.of(snapshot, delta, delta, gap, recovery, trade));

        assertFalse(first.failed());
        assertEquals(first.finalState(), second.finalState());
        assertEquals(first.digest(), second.digest());
        assertTrue(first.trace().stream().anyMatch(trace -> trace.projectionDecision().equals("DUPLICATE_IGNORED")));
        assertTrue(first.finalState().pendingResyncRequests().isEmpty());
        var book = first.finalState().books().get(snapshot.streamKey());
        assertEquals(OrderBookStatus.HEALTHY, book.status());
        assertEquals(5, book.asOfSequence().orElseThrow().value());
        assertTrue(first.finalState().aggregations().get(trade.streamKey()).ticker().isPresent());

        MarketDataViewEnvelope envelope = MarketDataViewEnvelope.from(new MarketDataReadOnlyView.OrderBook(book));
        var decision = new ReadOnlySubscriptionBackpressurePolicy().evaluate(
                SubscriberCursor.fresh(envelope.streamKey()), envelope, new SubscriptionPolicy(1, BackpressureStrategy.DROP_AND_RESNAPSHOT)
        );
        assertEquals(SubscriptionOutcome.PUBLISHED, decision.outcome());
        assertEquals(5, decision.nextCursor().orElseThrow().lastDeliveredVersion().orElseThrow());
    }

    @Test
    void marketDataFoundationHasNoPublicTransportOrTradingCoreDependency() throws IOException {
        // Phase final gate：foundation package 不得偷接 controller/WebSocket/provider 或任何交易資金核心。
        Path sourceRoot = Path.of("src/main/java/com/lumix/marketdata");
        List<String> forbidden = List.of(
                "import java.net.", "import org.springframework.web", "import org.springframework.messaging",
                "import com.lumix.trading", "import com.lumix.ledger", "import com.lumix.balance", "import com.lumix.position",
                "import com.lumix.reservation", "import com.lumix.settlement", "import com.lumix.wallet"
        );
        try (var paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(value -> value.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                for (String token : forbidden) {
                    assertFalse(source.contains(token), () -> path + " must not contain forbidden token: " + token);
                }
            }
        }
    }

    private static MarketDataReplayResult replay(DeterministicMarketDataReplayer replayer, List<NormalizedMarketDataEvent> events) {
        return replayer.replay(new MarketDataReplayInput(MarketDataReplayState.empty(), events, BASE_TIME.plusSeconds(30)));
    }

    private static NormalizedMarketDataEvent bookSnapshot(long sequence, String bid, String ask) {
        return event(MarketDataChannel.BOOK, MarketDataEventType.BOOK_SNAPSHOT, sequence,
                new BookSnapshotPayload(List.of(new BookLevel(price(bid), quantity("100000000"))), List.of(new BookLevel(price(ask), quantity("100000000")))));
    }

    private static NormalizedMarketDataEvent bookDelta(long sequence, String bid) {
        return event(MarketDataChannel.BOOK, MarketDataEventType.BOOK_DELTA, sequence,
                new BookDeltaPayload(List.of(new BookLevel(price(bid), quantity("200000000"))), List.of()));
    }

    private static NormalizedMarketDataEvent trade(long sequence, String price) {
        return new NormalizedMarketDataEvent(
                new MarketDataSource("fixture-source"), MarketDataChannel.TRADES, new InstrumentId("ETH-USDT"), MarketDataEventType.TRADE,
                new Sequence(sequence), BASE_TIME.plusSeconds(sequence), BASE_TIME.plusSeconds(sequence + 1), SchemaVersion.V1, PRECISION,
                new TradePayload("trade-" + sequence, price(price), quantity("100000000"))
        );
    }

    private static NormalizedMarketDataEvent event(MarketDataChannel channel, MarketDataEventType type, long sequence, MarketDataPayload payload) {
        return new NormalizedMarketDataEvent(
                new MarketDataSource("fixture-source"), channel, new InstrumentId("BTC-USDT"), type, new Sequence(sequence),
                BASE_TIME.plusSeconds(sequence), BASE_TIME.plusSeconds(sequence + 1), SchemaVersion.V1, PRECISION, payload
        );
    }

    private static DecimalPrice price(String value) { return DecimalPrice.fromWire(value, PRECISION); }
    private static AtomicQuantity quantity(String value) { return AtomicQuantity.fromWire(value, PRECISION); }
}
