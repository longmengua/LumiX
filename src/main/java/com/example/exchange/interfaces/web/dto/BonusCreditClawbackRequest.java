/*
 * 檔案用途：Web DTO，承接體驗金追回 request。
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BonusCreditClawbackRequest(
        @NotNull Long uid,
        @NotBlank String asset,
        @NotNull @DecimalMin(value = "0.000000000000000001") BigDecimal amount,
        String refId
) {
}
