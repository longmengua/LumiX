package com.lumix.marketdata.contract;

/**
 * 讓 serialization package 的測試重用固定 fixture，同時不把 fixture 變成 production domain 能力。
 */
public final class MarketDataEventFixtureAccess {

    private MarketDataEventFixtureAccess() {
    }

    public static BookSnapshotPayload bookSnapshot() {
        return MarketDataEventFixture.bookSnapshot();
    }

    public static BookDeltaPayload bookDelta() {
        return MarketDataEventFixture.bookDelta();
    }

    public static TradePayload trade() {
        return MarketDataEventFixture.trade();
    }

    public static TickerPayload ticker() {
        return MarketDataEventFixture.ticker();
    }

    public static NormalizedMarketDataEvent event(MarketDataPayload payload) {
        return MarketDataEventFixture.event(payload);
    }
}
