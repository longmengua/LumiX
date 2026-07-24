package com.lumix.marketdata.contract;

import java.time.Instant;
import java.util.List;

/**
 * P21-T02 contract 測試使用的固定 fixture。
 *
 * <p>所有時間與數值都明確指定，確保測試不會因 wall clock、隨機值或前端 mock 資料而改變結果。</p>
 */
final class MarketDataEventFixture {

    static final InstrumentPrecision PRECISION = new InstrumentPrecision(2, 8, 20);
    static final Instant SOURCE_TIME = Instant.parse("2026-07-25T00:00:00Z");
    static final Instant RECEIVED_TIME = Instant.parse("2026-07-25T00:00:01Z");

    private MarketDataEventFixture() {
    }

    static DecimalPrice price(String value) {
        return DecimalPrice.fromWire(value, PRECISION);
    }

    static AtomicQuantity quantity(String value) {
        return AtomicQuantity.fromWire(value, PRECISION);
    }

    static TradePayload trade() {
        return new TradePayload("trade-001", price("100.25"), quantity("125000000"));
    }

    static BookSnapshotPayload bookSnapshot() {
        return new BookSnapshotPayload(
                List.of(new BookLevel(price("100.25"), quantity("125000000"))),
                List.of(new BookLevel(price("100.26"), quantity("200000000")))
        );
    }

    static BookDeltaPayload bookDelta() {
        return new BookDeltaPayload(
                List.of(new BookLevel(price("100.25"), quantity("0"))),
                List.of(new BookLevel(price("100.27"), quantity("300000000")))
        );
    }

    static TickerPayload ticker() {
        return new TickerPayload(price("100.25"), quantity("900000000"));
    }

    static NormalizedMarketDataEvent event(MarketDataPayload payload) {
        return new NormalizedMarketDataEvent(
                new MarketDataSource("fixture-source"),
                channelFor(payload),
                new InstrumentId("BTC-USDT"),
                payload.eventType(),
                new Sequence(42),
                SOURCE_TIME,
                RECEIVED_TIME,
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
}
