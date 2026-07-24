package com.lumix.marketdata.contract;

/**
 * 單筆 normalized trade input。
 *
 * <p>這不是 trading-core 的 fill，也不會改變任何 order、position 或 settlement 狀態。</p>
 */
public record TradePayload(String tradeId, DecimalPrice price, AtomicQuantity quantity) implements MarketDataPayload {

    public TradePayload {
        tradeId = MarketDataContractValidation.requireText(
                tradeId,
                "tradeId",
                MarketDataContractValidation.OPAQUE_IDENTIFIER_PATTERN
        );
        MarketDataContractValidation.requireValue(price, "price");
        MarketDataContractValidation.requireValue(quantity, "quantity");
        if (!price.isPositive() || !quantity.isPositive()) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INVALID_DECIMAL,
                    "trade price and quantity must be positive"
            );
        }
    }

    @Override
    public MarketDataEventType eventType() {
        return MarketDataEventType.TRADE;
    }

    @Override
    public void validateAgainst(InstrumentPrecision precision) {
        DecimalPrice.fromWire(price.toWireString(), precision);
        AtomicQuantity.fromWire(quantity.toWireString(), precision);
    }

    @Override
    public String canonicalForm() {
        return "TRADE|" + BookSnapshotPayload.token(tradeId)
                + BookSnapshotPayload.token(price.toWireString())
                + BookSnapshotPayload.token(quantity.toWireString());
    }
}
