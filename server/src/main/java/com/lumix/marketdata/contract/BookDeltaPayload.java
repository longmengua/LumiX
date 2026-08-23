package com.lumix.marketdata.contract;

import java.util.List;

/**
 * 唯讀 book delta 的原始輸入 payload。
 *
 * <p>delta 套用、gap 偵測與 resync 一律留給後續 task；此型別不保存任何 mutable order-book state。</p>
 */
public record BookDeltaPayload(List<BookLevel> bidUpdates, List<BookLevel> askUpdates) implements MarketDataPayload {

    public BookDeltaPayload {
        bidUpdates = BookSnapshotPayload.copyLevels(bidUpdates, "bidUpdates");
        askUpdates = BookSnapshotPayload.copyLevels(askUpdates, "askUpdates");
        // delta 同樣必須在 reducer 前受限，否則單筆更新仍可造成排序與聚合資源耗盡。
        bidUpdates = OrderBookLevelLimits.requirePayloadSideWithinLimit(bidUpdates, "bidUpdates");
        askUpdates = OrderBookLevelLimits.requirePayloadSideWithinLimit(askUpdates, "askUpdates");
    }

    @Override
    public MarketDataEventType eventType() {
        return MarketDataEventType.BOOK_DELTA;
    }

    @Override
    public void validateAgainst(InstrumentPrecision precision) {
        MarketDataContractValidation.requireValue(precision, "precision");
        bidUpdates.forEach(level -> BookSnapshotPayload.validateLevel(level, precision));
        askUpdates.forEach(level -> BookSnapshotPayload.validateLevel(level, precision));
    }

    @Override
    public String canonicalForm() {
        return "BOOK_DELTA|" + BookSnapshotPayload.canonicalLevels(bidUpdates)
                + "|" + BookSnapshotPayload.canonicalLevels(askUpdates);
    }
}
