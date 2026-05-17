package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record LiquidationRequest(
        long uid,
        @NotBlank String symbol,
        @NotNull @Positive BigDecimal markPrice
) {}
