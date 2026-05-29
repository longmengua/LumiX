/*
 * 檔案用途：Web DTO，承接鏈上 / 銀行入金 callback。
 */
package com.example.exchange.interfaces.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public record DepositCallbackRequest(
        @NotNull(message = "uid 不可為空")
        Long uid,

        @NotNull(message = "amount 不可為空")
        @DecimalMin(value = "0.0001", message = "amount 必須 >= 0.0001")
        BigDecimal amount,

        @NotBlank(message = "externalRef 不可為空")
        String externalRef
) {
}
