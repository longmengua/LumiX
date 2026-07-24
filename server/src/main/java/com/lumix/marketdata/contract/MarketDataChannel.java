package com.lumix.marketdata.contract;

/**
 * 來源端可比較 sequence 的行情 channel。
 *
 * <p>channel 不代表 transport topic；它僅與 source、instrument 一起構成 stream key。</p>
 */
public enum MarketDataChannel {
    BOOK,
    TRADES,
    TICKER;

    public static MarketDataChannel fromWire(String value) {
        try {
            return MarketDataChannel.valueOf(value);
        } catch (RuntimeException exception) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INCOMPLETE_STREAM_KEY,
                    "channel is unknown or missing"
            );
        }
    }
}
