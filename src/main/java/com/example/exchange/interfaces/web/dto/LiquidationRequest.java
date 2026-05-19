/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record LiquidationRequest(
        long uid,
        @NotBlank String symbol,
        BigDecimal markPrice
) {}
