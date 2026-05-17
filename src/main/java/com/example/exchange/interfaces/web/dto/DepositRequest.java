/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public record DepositRequest(
        @NotNull(message = "uid 不可為空")
        Long uid,

        @NotNull(message = "amount 不可為空")
        @DecimalMin(value = "0.0001", message = "amount 必須 >= 0.0001")
        BigDecimal amount
) {}
