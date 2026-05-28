/*
 * 檔案用途：REST request DTO，承載做市商 per-symbol risk limit 設定。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.dto.MarketMakerRiskLimit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record MarketMakerRiskLimitRequest(
        @NotBlank String symbol,
        @DecimalMin("0") BigDecimal maxLongNotional,
        @DecimalMin("0") BigDecimal maxShortNotional,
        @DecimalMin("0") BigDecimal maxOrderNotional,
        @DecimalMin("0") BigDecimal maxSlippageRate,
        boolean killSwitch
) {
    public MarketMakerRiskLimit toLimit() {
        return new MarketMakerRiskLimit(
                symbol == null ? null : symbol.trim().toUpperCase(),
                maxLongNotional,
                maxShortNotional,
                maxOrderNotional,
                maxSlippageRate,
                killSwitch
        );
    }
}
