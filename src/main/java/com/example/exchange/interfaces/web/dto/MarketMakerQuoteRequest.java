/*
 * 檔案用途：REST request DTO，承載做市商雙邊 quote command。
 */
package com.example.exchange.interfaces.web.dto;

import com.example.exchange.domain.model.dto.MarketMakerQuoteCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MarketMakerQuoteRequest(
        @NotBlank String marketMakerId,
        @Positive long uid,
        @NotBlank String symbol,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal bidPrice,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal bidQuantity,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal askPrice,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal askQuantity,
        String refId
) {
    public MarketMakerQuoteCommand toCommand() {
        return new MarketMakerQuoteCommand(
                marketMakerId == null ? null : marketMakerId.trim(),
                uid,
                symbol == null ? null : symbol.trim().toUpperCase(),
                bidPrice,
                bidQuantity,
                askPrice,
                askQuantity,
                refId == null ? null : refId.trim()
        );
    }
}
