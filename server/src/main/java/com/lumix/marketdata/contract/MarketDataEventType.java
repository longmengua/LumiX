package com.lumix.marketdata.contract;

/**
 * T02 可接受的 provider-neutral payload 類型。
 *
 * <p>此列舉不含 provider 專屬類型；未知 type 在 wire decode 時拒絕，不能嘗試推測相容 payload。</p>
 */
public enum MarketDataEventType {
    BOOK_SNAPSHOT,
    BOOK_DELTA,
    TRADE,
    TICKER;

    public static MarketDataEventType fromWire(String value) {
        try {
            return MarketDataEventType.valueOf(value);
        } catch (RuntimeException exception) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.UNKNOWN_EVENT_TYPE,
                    "eventType is unknown or missing"
            );
        }
    }
}
