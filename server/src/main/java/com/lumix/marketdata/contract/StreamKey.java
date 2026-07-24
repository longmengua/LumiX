package com.lumix.marketdata.contract;

/**
 * sequence 唯一可比較的來源流範圍。
 *
 * <p>禁止以全域 sequence 比較不同來源、channel 或 instrument，避免錯把互不相關的 feed 排成同一條流。</p>
 */
public record StreamKey(MarketDataSource source, MarketDataChannel channel, InstrumentId instrumentId) {

    public StreamKey {
        if (source == null || channel == null || instrumentId == null) {
            throw MarketDataContractValidation.rejected(
                    MarketDataRejectionReason.INCOMPLETE_STREAM_KEY,
                    "source, channel and instrumentId must all be present"
            );
        }
    }
}
