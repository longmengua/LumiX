package com.lumix.marketdata.contract.serialization;

import java.util.List;

/**
 * 對應 sealed domain payload 的 provider-neutral wire form。
 *
 * <p>所有價格與 quantity 都是字串，刻意避免 JSON number、locale 與 scientific notation 對精度造成變化。</p>
 */
public sealed interface MarketDataPayloadWire permits MarketDataPayloadWire.BookSnapshot,
        MarketDataPayloadWire.BookDelta, MarketDataPayloadWire.Trade, MarketDataPayloadWire.Ticker {

    record BookLevel(String price, String quantityAtoms) {
    }

    record BookSnapshot(List<BookLevel> bids, List<BookLevel> asks) implements MarketDataPayloadWire {
        public BookSnapshot {
            bids = List.copyOf(bids);
            asks = List.copyOf(asks);
        }
    }

    record BookDelta(List<BookLevel> bidUpdates, List<BookLevel> askUpdates) implements MarketDataPayloadWire {
        public BookDelta {
            bidUpdates = List.copyOf(bidUpdates);
            askUpdates = List.copyOf(askUpdates);
        }
    }

    record Trade(String tradeId, String price, String quantityAtoms) implements MarketDataPayloadWire {
    }

    record Ticker(String lastPrice, String baseVolumeAtoms) implements MarketDataPayloadWire {
    }
}
