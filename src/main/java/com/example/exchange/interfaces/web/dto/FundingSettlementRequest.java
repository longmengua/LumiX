/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record FundingSettlementRequest(
        long uid,
        @NotBlank String symbol,
        @NotNull @Positive BigDecimal markPrice,
        @NotNull BigDecimal fundingRate
) {}
