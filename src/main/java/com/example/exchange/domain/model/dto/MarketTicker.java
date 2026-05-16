package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketTicker(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal bestBid,
        BigDecimal bestAsk,
        BigDecimal volume24h,
        BigDecimal high24h,
        BigDecimal low24h,
        Instant updatedAt
) {}
