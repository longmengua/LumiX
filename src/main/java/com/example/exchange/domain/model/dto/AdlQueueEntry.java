package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdlQueueEntry(
        String liquidationId,
        long uid,
        String symbol,
        BigDecimal amount,
        Instant ts
) {}
