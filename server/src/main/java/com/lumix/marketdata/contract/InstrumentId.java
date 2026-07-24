package com.lumix.marketdata.contract;

/**
 * 不帶展示或交易設定的穩定 instrument identity。
 *
 * <p>刻意不做 trim 或大小寫修正，避免把上游的模糊輸入靜默變成另一個事件 identity。</p>
 */
public record InstrumentId(String value) {

    public InstrumentId {
        value = MarketDataContractValidation.requireText(
                value,
                "instrumentId",
                MarketDataContractValidation.INSTRUMENT_PATTERN
        );
    }
}
