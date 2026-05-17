package com.example.exchange.domain.event;

import com.example.exchange.domain.model.entity.Symbol;

import java.math.BigDecimal;
import java.time.Instant;

public record FundingSettled(
        long uid,
        Symbol symbol,
        BigDecimal markPrice,
        BigDecimal fundingRate,
        BigDecimal cashflow,
        String settlementId,
        Instant ts
) {}
