/*
 * File purpose: Request payload for admin-issued MVP test funds.
 */
package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TestFundsAirdropRequest(
        @NotNull(message = "uid is required")
        Long uid,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0001", message = "amount must be >= 0.0001")
        BigDecimal amount
) {
}
