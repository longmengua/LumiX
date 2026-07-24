package com.lumix.marketdata.contract;

/**
 * Ticker 的 normalized input。
 *
 * <p>window 聚合與 stale 判定均不在 T02；此 payload 只保存精確的 last price 與 base volume 輸入。</p>
 */
public record TickerPayload(DecimalPrice lastPrice, AtomicQuantity baseVolume) implements MarketDataPayload {

    public TickerPayload {
        MarketDataContractValidation.requireValue(lastPrice, "lastPrice");
        MarketDataContractValidation.requireValue(baseVolume, "baseVolume");
        if (!lastPrice.isPositive()) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INVALID_DECIMAL,
                    "ticker lastPrice must be positive"
            );
        }
    }

    @Override
    public MarketDataEventType eventType() {
        return MarketDataEventType.TICKER;
    }

    @Override
    public void validateAgainst(InstrumentPrecision precision) {
        DecimalPrice.fromWire(lastPrice.toWireString(), precision);
        AtomicQuantity.fromWire(baseVolume.toWireString(), precision);
    }

    @Override
    public String canonicalForm() {
        return "TICKER|" + BookSnapshotPayload.token(lastPrice.toWireString())
                + BookSnapshotPayload.token(baseVolume.toWireString());
    }
}
