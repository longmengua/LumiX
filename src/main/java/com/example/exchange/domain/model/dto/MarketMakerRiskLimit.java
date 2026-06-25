/*
 * 檔案用途：做市商風控限制 DTO，描述單一 symbol 的對沖與風險邊界。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerRiskLimit {

    private final String symbol;

    private final BigDecimal maxLongNotional;

    private final BigDecimal maxShortNotional;

    private final BigDecimal maxOrderNotional;

    private final BigDecimal maxSlippageRate;

    private final boolean killSwitch;

    public MarketMakerRiskLimit(String symbol, BigDecimal maxLongNotional, BigDecimal maxShortNotional, BigDecimal maxOrderNotional, BigDecimal maxSlippageRate, boolean killSwitch) {
        maxLongNotional = defaultZero(maxLongNotional);
        maxShortNotional = defaultZero(maxShortNotional);
        maxOrderNotional = defaultZero(maxOrderNotional);
        maxSlippageRate = defaultZero(maxSlippageRate);
    
        this.symbol = symbol;
        this.maxLongNotional = maxLongNotional;
        this.maxShortNotional = maxShortNotional;
        this.maxOrderNotional = maxOrderNotional;
        this.maxSlippageRate = maxSlippageRate;
        this.killSwitch = killSwitch;
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal maxLongNotional() {
        return maxLongNotional;
    }

    public BigDecimal maxShortNotional() {
        return maxShortNotional;
    }

    public BigDecimal maxOrderNotional() {
        return maxOrderNotional;
    }

    public BigDecimal maxSlippageRate() {
        return maxSlippageRate;
    }

    public boolean killSwitch() {
        return killSwitch;
    }
}
