package com.lumix.marketdata.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lumix.marketdata.book.OrderBookStatus;
import com.lumix.marketdata.book.ReadOnlyOrderBookProjection;
import com.lumix.marketdata.contract.AtomicQuantity;
import com.lumix.marketdata.contract.BookLevel;
import com.lumix.marketdata.contract.DecimalPrice;
import com.lumix.marketdata.contract.InstrumentId;
import com.lumix.marketdata.contract.InstrumentPrecision;
import com.lumix.marketdata.contract.MarketDataChannel;
import com.lumix.marketdata.contract.MarketDataEventType;
import com.lumix.marketdata.contract.MarketDataSource;
import com.lumix.marketdata.contract.NormalizedMarketDataEvent;
import com.lumix.marketdata.contract.SchemaVersion;
import com.lumix.marketdata.contract.Sequence;
import com.lumix.marketdata.contract.StreamKey;
import com.lumix.marketdata.contract.TradePayload;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadOnlyMarketDataQueryContractTest {

    private static final InstrumentPrecision PRECISION = new InstrumentPrecision(2, 8, 20);
    private static final Instant TIME = Instant.parse("2026-08-01T00:00:00Z");
    private final ReadOnlySubscriptionBackpressurePolicy backpressure = new ReadOnlySubscriptionBackpressurePolicy();

    @Test
    void queryOnlyPublishesImmutableNewerEnvelopeAndUnknownStreamIsEmpty() {
        // internal query 不能創造不存在的行情，也不能讓舊 projection 回退覆寫較新版本。
        MarketDataViewEnvelope versionOne = envelope("BTC-USDT", 1, OrderBookStatus.HEALTHY);
        MarketDataViewEnvelope versionTwo = envelope("BTC-USDT", 2, OrderBookStatus.HEALTHY);
        InMemoryReadOnlyMarketDataViews views = InMemoryReadOnlyMarketDataViews.empty().publish(versionTwo).publish(versionOne);

        assertEquals(versionTwo, views.current(versionTwo.streamKey()).orElseThrow());
        assertTrue(views.current(new StreamKey(new MarketDataSource("fixture-source"), MarketDataChannel.BOOK, new InstrumentId("ETH-USDT"))).isEmpty());
    }

    @Test
    void healthyContiguousVersionsPublishButDuplicateNeverRepublishes() {
        // consumer local cursor 必須逐 version 連續前進；同版重送不可產生第二份 update。
        MarketDataViewEnvelope one = envelope("BTC-USDT", 1, OrderBookStatus.HEALTHY);
        MarketDataViewEnvelope two = envelope("BTC-USDT", 2, OrderBookStatus.HEALTHY);
        SubscriptionPolicy policy = new SubscriptionPolicy(2, BackpressureStrategy.DROP_AND_RESNAPSHOT);
        SubscriptionDecision first = backpressure.evaluate(SubscriberCursor.fresh(one.streamKey()), one, policy);
        SubscriptionDecision second = backpressure.evaluate(first.nextCursor().orElseThrow(), two, policy);
        SubscriptionDecision duplicate = backpressure.evaluate(second.nextCursor().orElseThrow(), two, policy);

        assertEquals(SubscriptionOutcome.PUBLISHED, first.outcome());
        assertEquals(SubscriptionOutcome.PUBLISHED, second.outcome());
        assertEquals(SubscriptionOutcome.DUPLICATE_IGNORED, duplicate.outcome());
        assertTrue(duplicate.envelope().isEmpty());
    }

    @Test
    void versionGapAndNonHealthyViewRequireResnapshotInsteadOfSilentLoss() {
        // gap 或 degraded 即使 view 本身可查詢，也不能被 stream consumer 當成連續 live update。
        MarketDataViewEnvelope one = envelope("BTC-USDT", 1, OrderBookStatus.HEALTHY);
        MarketDataViewEnvelope gap = envelope("BTC-USDT", 3, OrderBookStatus.HEALTHY);
        MarketDataViewEnvelope degraded = envelope("BTC-USDT", 2, OrderBookStatus.DEGRADED);
        SubscriptionPolicy policy = new SubscriptionPolicy(2, BackpressureStrategy.DROP_AND_RESNAPSHOT);
        SubscriberCursor afterOne = backpressure.evaluate(SubscriberCursor.fresh(one.streamKey()), one, policy).nextCursor().orElseThrow();

        SubscriptionDecision gapDecision = backpressure.evaluate(afterOne, gap, policy);
        SubscriptionDecision degradedDecision = backpressure.evaluate(afterOne, degraded, policy);
        assertEquals(SubscriptionOutcome.RESNAPSHOT_REQUIRED, gapDecision.outcome());
        assertEquals(SubscriptionReason.VERSION_GAP, gapDecision.reason());
        assertEquals(SubscriptionOutcome.RESNAPSHOT_REQUIRED, degradedDecision.outcome());
        assertEquals(SubscriptionReason.NON_HEALTHY_VIEW, degradedDecision.reason());
        assertEquals(0, degradedDecision.nextCursor().orElseThrow().pendingUpdates());
    }

    @Test
    void capacityOverflowDisconnectsOnlyTheSlowConsumer() {
        // policy 只回傳單一 consumer 的斷線結果，沒有共享 queue，因此慢 consumer 不會阻塞其他 stream/reducer。
        MarketDataViewEnvelope one = envelope("BTC-USDT", 1, OrderBookStatus.HEALTHY);
        MarketDataViewEnvelope two = envelope("BTC-USDT", 2, OrderBookStatus.HEALTHY);
        SubscriptionPolicy policy = new SubscriptionPolicy(1, BackpressureStrategy.DISCONNECT_AND_RESNAPSHOT);
        SubscriberCursor afterOne = backpressure.evaluate(SubscriberCursor.fresh(one.streamKey()), one, policy).nextCursor().orElseThrow();
        SubscriptionDecision overflow = backpressure.evaluate(afterOne, two, policy);

        assertEquals(SubscriptionOutcome.DISCONNECTED_AND_RESNAPSHOT, overflow.outcome());
        assertEquals(SubscriptionReason.CONSUMER_CAPACITY_EXCEEDED, overflow.reason());
        assertFalse(overflow.nextCursor().isPresent());
    }

    private static MarketDataViewEnvelope envelope(String instrument, long version, OrderBookStatus status) {
        NormalizedMarketDataEvent identityEvent = new NormalizedMarketDataEvent(
                new MarketDataSource("fixture-source"), MarketDataChannel.TRADES, new InstrumentId(instrument), MarketDataEventType.TRADE,
                new Sequence(version), TIME.plusSeconds(version), TIME.plusSeconds(version + 1), SchemaVersion.V1, PRECISION,
                new TradePayload("identity-" + version, price("100.00"), quantity("1"))
        );
        StreamKey streamKey = new StreamKey(new MarketDataSource("fixture-source"), MarketDataChannel.BOOK, new InstrumentId(instrument));
        ReadOnlyOrderBookProjection projection = new ReadOnlyOrderBookProjection(
                streamKey, Optional.of(new Sequence(version)), Optional.of(TIME.plusSeconds(version)), Optional.of(identityEvent.identity()),
                List.of(new BookLevel(price("100.00"), quantity("1"))), List.of(new BookLevel(price("100.01"), quantity("1"))), status
        );
        return MarketDataViewEnvelope.from(new MarketDataReadOnlyView.OrderBook(projection));
    }

    private static DecimalPrice price(String value) {
        return DecimalPrice.fromWire(value, PRECISION);
    }

    private static AtomicQuantity quantity(String value) {
        return AtomicQuantity.fromWire(value, PRECISION);
    }
}
