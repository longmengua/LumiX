package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketKline(
        String symbol,
        String interval,
        Instant openTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {}
