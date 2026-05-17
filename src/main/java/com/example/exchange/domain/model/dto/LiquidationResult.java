package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LiquidationResult(
        long uid,
        String symbol,
        boolean liquidated,
        BigDecimal markPrice,
        BigDecimal maintenanceMargin,
        BigDecimal equity,
        BigDecimal closedQty,
        BigDecimal realizedPnl,
        BigDecimal insuranceFundCovered,
        BigDecimal adlCovered,
        String liquidationId,
        Instant ts
) {}
