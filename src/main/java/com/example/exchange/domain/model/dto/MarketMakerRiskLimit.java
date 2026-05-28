/*
 * 檔案用途：做市商風控限制 DTO，描述單一 symbol 的對沖與風險邊界。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record MarketMakerRiskLimit(
        String symbol,
        BigDecimal maxLongNotional,
        BigDecimal maxShortNotional,
        BigDecimal maxOrderNotional,
        BigDecimal maxSlippageRate,
        boolean killSwitch
) {
    public MarketMakerRiskLimit {
        maxLongNotional = defaultZero(maxLongNotional);
        maxShortNotional = defaultZero(maxShortNotional);
        maxOrderNotional = defaultZero(maxOrderNotional);
        maxSlippageRate = defaultZero(maxSlippageRate);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
