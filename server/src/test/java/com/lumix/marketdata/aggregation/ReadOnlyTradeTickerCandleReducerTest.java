package com.lumix.marketdata.aggregation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.InstrumentId;
import com.lumix.marketdata.contract.InstrumentPrecision;
import com.lumix.marketdata.contract.MarketDataChannel;
import com.lumix.marketdata.contract.MarketDataSource;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.contract.SchemaVersion;
import com.lumix.marketdata.contract.Sequence;
import com.lumix.marketdata.contract.TradePayload;
import com.lumix.marketdata.policy.FeedHealth;
import com.lumix.marketdata.policy.MarketDataAdmissionResult;
import com.lumix.marketdata.policy.MarketDataStalePolicy;
import com.lumix.marketdata.policy.MarketDataStreamAdmissionPolicy;
import com.lumix.marketdata.policy.MarketDataStreamCursor;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadOnlyTradeTickerCandleReducerTest {

    private static final InstrumentPrecision PRECISION = new InstrumentPrecision(2, 8, 20);
    private static final Instant BASE_TIME = Instant.parse("2026-08-01T00:00:00Z");
    private final MarketDataStreamAdmissionPolicy admissionPolicy = new MarketDataStreamAdmissionPolicy(
            new MarketDataStalePolicy(Duration.ofSeconds(30))
    );
    private final ReadOnlyTradeTickerCandleReducer reducer = new ReadOnlyTradeTickerCandleReducer();

    @Test
    void acceptedTradesUseSourceTimeForDeterministicTickerAndOhlcv() {
        // 保護不變式：open/high/low/close 與所有 volume 必須只由固定 source time/decimal fixture 得出。
        NormalizedMarketDataEvent first = event("BTC-USDT", 1, BASE_TIME, BASE_TIME.plusSeconds(1), "100.00", "100000000");
        NormalizedMarketDataEvent second = event("BTC-USDT", 2, BASE_TIME.plusSeconds(20), BASE_TIME.plusSeconds(21), "99.00", "200000000");
        NormalizedMarketDataEvent third = event("BTC-USDT", 3, BASE_TIME.plusSeconds(50), BASE_TIME.plusSeconds(51), "101.00", "300000000");

        TradeAggregationProjection projection = apply(Optional.empty(), first, Optional.empty()).projection();
        projection = apply(Optional.of(projection), second, Optional.of(cursorFor(first))).projection();
        TradeAggregationResult result = apply(Optional.of(projection), third, Optional.of(cursorFor(second)));

        assertTrue(result.applied());
        TickerView ticker = result.projection().ticker().orElseThrow();
        CandleView candle = result.projection().candles().get(CandleInterval.ONE_MINUTE);
        assertEquals("100.00", ticker.open().toWireString());
        assertEquals("101.00", ticker.high().toWireString());
        assertEquals("99.00", ticker.low().toWireString());
        assertEquals("101.00", ticker.last().toWireString());
        assertEquals("600000000", ticker.baseVolume().toWireString());
        assertEquals("6010000000000", ticker.quoteVolume().atoms().toString());
        assertEquals(10, ticker.quoteVolume().scale());
        assertEquals(BASE_TIME.plusSeconds(50).minus(Duration.ofHours(24)), ticker.windowStartInclusive());
        assertEquals(BASE_TIME.plusSeconds(50), ticker.windowEndInclusive());
        assertEquals("100.00", candle.open().toWireString());
        assertEquals("101.00", candle.high().toWireString());
        assertEquals("99.00", candle.low().toWireString());
        assertEquals("101.00", candle.close().toWireString());
        assertEquals(3, candle.tradeCount());
        assertEquals(BASE_TIME, candle.window().startInclusive());
        assertEquals(BASE_TIME.plusSeconds(60), candle.window().endExclusive());
    }

    @Test
    void duplicateAdmissionNeverChangesPublishedVolumeOrAsOfMetadata() {
        // duplicate 即使有新的 received timestamp，也不能被重新計入 ticker 或 candle。
        NormalizedMarketDataEvent event = event("BTC-USDT", 1, BASE_TIME, BASE_TIME.plusSeconds(1), "100.00", "100000000");
        TradeAggregationProjection applied = apply(Optional.empty(), event, Optional.empty()).projection();
        MarketDataAdmissionResult duplicateAdmission = admissionPolicy.evaluate(
                event, Optional.of(cursorFor(event)), BASE_TIME.plusSeconds(2)
        );

        TradeAggregationResult duplicate = reducer.reduce(Optional.of(applied), event, duplicateAdmission);

        assertEquals(TradeAggregationDecision.DUPLICATE_IGNORED, duplicate.decision());
        assertEquals(TradeAggregationReason.DUPLICATE_ADMISSION, duplicate.reason());
        assertEquals(applied.ticker(), duplicate.projection().ticker());
        assertEquals(applied.candles(), duplicate.projection().candles());
        assertEquals(applied.asOfSequence(), duplicate.projection().asOfSequence());
    }

    @Test
    void gapOrStaleAdmissionFreezesAggregationAndExposesNonHealthyStatus() {
        // 缺號與 stale 不能繼續被 consumer 當成完整 ticker/candle；兩者都不得修改既有 OHLCV。
        NormalizedMarketDataEvent first = event("BTC-USDT", 1, BASE_TIME, BASE_TIME.plusSeconds(1), "100.00", "100000000");
        TradeAggregationProjection applied = apply(Optional.empty(), first, Optional.empty()).projection();
        NormalizedMarketDataEvent gap = event("BTC-USDT", 3, BASE_TIME.plusSeconds(2), BASE_TIME.plusSeconds(3), "101.00", "100000000");
        MarketDataAdmissionResult gapAdmission = admissionPolicy.evaluate(gap, Optional.of(cursorFor(first)), BASE_TIME.plusSeconds(4));
        TradeAggregationResult gapResult = reducer.reduce(Optional.of(applied), gap, gapAdmission);

        assertEquals(TradeAggregationReason.ADMISSION_NOT_ACCEPTED, gapResult.reason());
        assertEquals(AggregationStatus.GAP_DETECTED, gapResult.projection().status());
        assertEquals(applied.ticker(), gapResult.projection().ticker());

        NormalizedMarketDataEvent stale = event("ETH-USDT", 1, BASE_TIME, BASE_TIME, "200.00", "100000000");
        MarketDataAdmissionResult staleAdmission = admissionPolicy.evaluate(stale, Optional.empty(), BASE_TIME.plusSeconds(30));
        TradeAggregationResult staleResult = reducer.reduce(Optional.empty(), stale, staleAdmission);
        assertEquals(FeedHealth.STALE, staleAdmission.nextCursor().health());
        assertEquals(TradeAggregationReason.FEED_NOT_HEALTHY, staleResult.reason());
        assertEquals(AggregationStatus.STALE, staleResult.projection().status());
        assertTrue(staleResult.projection().ticker().isEmpty());
    }

    @Test
    void sourceTimeDrivesCandleRolloverAndLateEventIsRejectedWithoutBackfill() {
        // reducer 沒有歷史 storage；一旦新 bucket 已發布，晚到 source event 不可回填改寫先前可觀察結果。
        NormalizedMarketDataEvent first = event("BTC-USDT", 1, BASE_TIME.plusSeconds(59), BASE_TIME.plusSeconds(60), "100.00", "100000000");
        NormalizedMarketDataEvent nextMinute = event("BTC-USDT", 2, BASE_TIME.plusSeconds(60), BASE_TIME.plusSeconds(61), "101.00", "100000000");
        TradeAggregationProjection projection = apply(Optional.empty(), first, Optional.empty()).projection();
        projection = apply(Optional.of(projection), nextMinute, Optional.of(cursorFor(first))).projection();

        CandleView oneMinute = projection.candles().get(CandleInterval.ONE_MINUTE);
        assertEquals(BASE_TIME.plusSeconds(60), oneMinute.window().startInclusive());
        assertEquals(BASE_TIME, projection.candles().get(CandleInterval.FIVE_MINUTES).window().startInclusive());

        NormalizedMarketDataEvent late = event("BTC-USDT", 3, BASE_TIME.plusSeconds(30), BASE_TIME.plusSeconds(62), "99.00", "100000000");
        TradeAggregationResult rejected = apply(Optional.of(projection), late, Optional.of(cursorFor(nextMinute)));
        assertEquals(TradeAggregationReason.LATE_SOURCE_EVENT, rejected.reason());
        assertEquals(AggregationStatus.DEGRADED, rejected.projection().status());
        assertEquals(projection.candles(), rejected.projection().candles());
    }

    @Test
    void tickerEvictsTradesOlderThanItsSourceTimeWindow() {
        // 24h rolling 視窗以 incoming source timestamp 切換；received time 不得影響淘汰邊界。
        NormalizedMarketDataEvent first = event("BTC-USDT", 1, BASE_TIME, BASE_TIME.plusSeconds(1), "100.00", "100000000");
        NormalizedMarketDataEvent afterWindow = event(
                "BTC-USDT", 2, BASE_TIME.plus(Duration.ofHours(24)).plusSeconds(1), BASE_TIME.plus(Duration.ofHours(24)).plusSeconds(2),
                "101.00", "200000000"
        );
        TradeAggregationProjection projection = apply(Optional.empty(), first, Optional.empty()).projection();
        projection = apply(Optional.of(projection), afterWindow, Optional.of(cursorFor(first))).projection();

        TickerView ticker = projection.ticker().orElseThrow();
        assertEquals(1, ticker.tradeCount());
        assertEquals("101.00", ticker.open().toWireString());
        assertEquals("200000000", ticker.baseVolume().toWireString());
    }

    @Test
    void projectionIsStreamIsolatedAndReplayIsDeterministic() {
        // 同一 ordered fixture 必須得到相等結果；跨 instrument projection 則不得共用 sequence 或成交量。
        NormalizedMarketDataEvent first = event("BTC-USDT", 1, BASE_TIME, BASE_TIME.plusSeconds(1), "100.00", "100000000");
        NormalizedMarketDataEvent second = event("BTC-USDT", 2, BASE_TIME.plusSeconds(1), BASE_TIME.plusSeconds(2), "101.00", "100000000");
        TradeAggregationProjection firstReplay = replay(first, second);
        TradeAggregationProjection secondReplay = replay(first, second);
        assertEquals(firstReplay, secondReplay);

        NormalizedMarketDataEvent otherInstrument = event("ETH-USDT", 2, BASE_TIME.plusSeconds(1), BASE_TIME.plusSeconds(2), "101.00", "100000000");
        TradeAggregationResult mismatch = reducer.reduce(
                Optional.of(firstReplay), otherInstrument, admissionPolicy.evaluate(otherInstrument, Optional.empty(), BASE_TIME.plusSeconds(3))
        );
        assertEquals(TradeAggregationReason.STREAM_KEY_MISMATCH, mismatch.reason());
        assertFalse(mismatch.applied());
    }

    @Test
    void quoteVolumeOverflowAndBoundedTickerStateFailClosed() {
        // 精度乘法與 bounded window 都不可靜默截斷；否則 volume 在 replay 中會被低估。
        InstrumentPrecision narrowPrecision = new InstrumentPrecision(2, 8, 4);
        NormalizedMarketDataEvent overflow = event(
                "BTC-USDT", 1, BASE_TIME, BASE_TIME.plusSeconds(1), "99.99", "9999", narrowPrecision
        );
        TradeAggregationResult overflowResult = reducer.reduce(
                Optional.empty(), overflow, admissionPolicy.evaluate(overflow, Optional.empty(), BASE_TIME.plusSeconds(2))
        );
        assertEquals(TradeAggregationReason.NUMERIC_OVERFLOW, overflowResult.reason());
        assertTrue(overflowResult.projection().ticker().isEmpty());

        TradeAggregationProjection projection = null;
        Optional<MarketDataStreamCursor> cursor = Optional.empty();
        for (int sequence = 1; sequence <= ReadOnlyTradeTickerCandleReducer.MAX_TICKER_WINDOW_TRADES; sequence++) {
            NormalizedMarketDataEvent trade = event(
                    "ETH-USDT", sequence, BASE_TIME, BASE_TIME.plusSeconds(1), "1.00", "1"
            );
            TradeAggregationResult result = apply(Optional.ofNullable(projection), trade, cursor);
            projection = result.projection();
            cursor = Optional.of(resultCursor(trade));
        }
        NormalizedMarketDataEvent tooMany = event(
                "ETH-USDT", ReadOnlyTradeTickerCandleReducer.MAX_TICKER_WINDOW_TRADES + 1,
                BASE_TIME, BASE_TIME.plusSeconds(1), "1.00", "1"
        );
        TradeAggregationResult capped = apply(Optional.of(projection), tooMany, cursor);
        assertEquals(TradeAggregationReason.WINDOW_TRADE_LIMIT_EXCEEDED, capped.reason());
        assertEquals(ReadOnlyTradeTickerCandleReducer.MAX_TICKER_WINDOW_TRADES, capped.projection().tickerWindowTrades().size());
    }

    private TradeAggregationProjection replay(NormalizedMarketDataEvent first, NormalizedMarketDataEvent second) {
        TradeAggregationProjection projection = apply(Optional.empty(), first, Optional.empty()).projection();
        return apply(Optional.of(projection), second, Optional.of(cursorFor(first))).projection();
    }

    private TradeAggregationResult apply(
            Optional<TradeAggregationProjection> previous,
            NormalizedMarketDataEvent event,
            Optional<MarketDataStreamCursor> previousCursor
    ) {
        MarketDataAdmissionResult admission = admissionPolicy.evaluate(event, previousCursor, event.receivedTimestamp().plusSeconds(1));
        return reducer.reduce(previous, event, admission);
    }

    private static MarketDataStreamCursor cursorFor(NormalizedMarketDataEvent event) {
        return resultCursor(event);
    }

    private static MarketDataStreamCursor resultCursor(NormalizedMarketDataEvent event) {
        return new MarketDataStreamCursor(
                event.streamKey(),
                Optional.of(new MarketDataStreamCursor.AcceptedMarketDataEvent(
                        event.sequence(), event.sourceTimestamp(), event.receivedTimestamp(), event.identity()
                )),
                FeedHealth.HEALTHY
        );
    }

    private static NormalizedMarketDataEvent event(
            String instrument,
            long sequence,
            Instant sourceTimestamp,
            Instant receivedTimestamp,
            String price,
            String quantity
    ) {
        return event(instrument, sequence, sourceTimestamp, receivedTimestamp, price, quantity, PRECISION);
    }

    private static NormalizedMarketDataEvent event(
            String instrument,
            long sequence,
            Instant sourceTimestamp,
            Instant receivedTimestamp,
            String price,
            String quantity,
            InstrumentPrecision precision
    ) {
        return new NormalizedMarketDataEvent(
                new MarketDataSource("fixture-source"), MarketDataChannel.TRADES, new InstrumentId(instrument),
                com.lumix.marketdata.contract.MarketDataEventType.TRADE, new Sequence(sequence), sourceTimestamp, receivedTimestamp,
                SchemaVersion.V1, precision,
                new TradePayload("trade-" + sequence, DecimalPrice.fromWire(price, precision), AtomicQuantity.fromWire(quantity, precision))
        );
    }
}
