package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record DepthDelta(
        String symbol,
        long version,
        List<PriceLevel> bids,
        List<PriceLevel> asks,
        Instant ts
) {}
