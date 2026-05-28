/*
 * 檔案用途：做市商 profile DTO，隔離做市商 uid、狀態與風控限制。
 */
package com.example.exchange.domain.model.dto;

import java.util.List;
import java.util.Optional;

public record MarketMakerProfile(
        String marketMakerId,
        long uid,
        boolean enabled,
        List<MarketMakerRiskLimit> riskLimits
) {
    public MarketMakerProfile {
        riskLimits = riskLimits == null ? List.of() : List.copyOf(riskLimits);
    }

    public Optional<MarketMakerRiskLimit> riskLimit(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase();
        return riskLimits.stream()
                .filter(limit -> normalized.equalsIgnoreCase(limit.symbol()))
                .findFirst();
    }
}
