package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record FeeCalculation(
        BigDecimal feeRate,
        BigDecimal fee,
        BigDecimal rebateRate,
        BigDecimal rebate
) {}
