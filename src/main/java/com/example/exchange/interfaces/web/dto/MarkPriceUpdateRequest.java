/*
 * 檔案用途：Web DTO，接收 mark/index price oracle 手動更新。
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MarkPriceUpdateRequest(
        @NotBlank String symbol,
        @NotNull @Positive BigDecimal markPrice,
        @NotNull @Positive BigDecimal indexPrice,
        String source
) {
}
