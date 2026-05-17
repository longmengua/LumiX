package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FundingSettlementResult(
        long uid,
        String symbol,
        BigDecimal markPrice,
        BigDecimal fundingRate,
        BigDecimal cashflow,
        String settlementId,
        boolean settled,
        Instant ts
) {}
