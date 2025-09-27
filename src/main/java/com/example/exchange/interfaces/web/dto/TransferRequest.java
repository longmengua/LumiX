package com.example.exchange.interfaces.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** 劃轉 API Request DTO */
public record TransferRequest(
        @NotNull Long uid,
        @NotBlank String symbol,
        boolean toIsolated,
        @DecimalMin("0.0001") BigDecimal amount
) {}
