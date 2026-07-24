package com.lumix.marketdata.contract;

/**
 * Provider-neutral 的受控來源識別字。
 *
 * <p>這個值物件只保存已由未來 adapter 邊界核准的來源代號，不含 SDK、endpoint、credential 或 provider payload。</p>
 */
public record MarketDataSource(String value) {

    public MarketDataSource {
        value = MarketDataContractValidation.requireText(value, "source", MarketDataContractValidation.SOURCE_PATTERN);
    }
}
