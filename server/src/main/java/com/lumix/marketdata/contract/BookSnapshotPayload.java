package com.lumix.marketdata.contract;

import java.util.List;

/**
 * 唯讀 book snapshot 的原始輸入 payload。
 *
 * <p>list 會 defensive copy，避免 event 建立後被呼叫端修改。book 可用性與 sequence 連續性不由本 task 判定。</p>
 */
public record BookSnapshotPayload(List<BookLevel> bids, List<BookLevel> asks) implements MarketDataPayload {

    public BookSnapshotPayload {
        bids = copyLevels(bids, "bids");
        asks = copyLevels(asks, "asks");
    }

    @Override
    public MarketDataEventType eventType() {
        return MarketDataEventType.BOOK_SNAPSHOT;
    }

    @Override
    public void validateAgainst(InstrumentPrecision precision) {
        MarketDataContractValidation.requireValue(precision, "precision");
        bids.forEach(level -> validateLevel(level, precision));
        asks.forEach(level -> validateLevel(level, precision));
    }

    @Override
    public String canonicalForm() {
        return "BOOK_SNAPSHOT|" + canonicalLevels(bids) + "|" + canonicalLevels(asks);
    }

    static List<BookLevel> copyLevels(List<BookLevel> levels, String fieldName) {
        MarketDataContractValidation.requireValue(levels, fieldName);
        try {
            List<BookLevel> copied = List.copyOf(levels);
            copied.forEach(level -> MarketDataContractValidation.requireValue(level, fieldName + " item"));
            return copied;
        } catch (NullPointerException exception) {
            throw MarketDataContractValidation.rejected(MarketDataRejectionReason.NULL_VALUE, fieldName + " must not contain null");
        }
    }

    static void validateLevel(BookLevel level, InstrumentPrecision precision) {
        DecimalPrice.fromWire(level.price().toWireString(), precision);
        AtomicQuantity.fromWire(level.quantity().toWireString(), precision);
    }

    static String canonicalLevels(List<BookLevel> levels) {
        return levels.stream()
                .map(level -> token(level.price().toWireString()) + token(level.quantity().toWireString()))
                .reduce("", String::concat);
    }

    static String token(String value) {
        return value.length() + ":" + value;
    }
}
