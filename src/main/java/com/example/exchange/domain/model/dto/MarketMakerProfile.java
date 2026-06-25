/*
 * 檔案用途：做市商 profile DTO，隔離做市商 uid、狀態與風控限制。
 */
package com.example.exchange.domain.model.dto;

import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerProfile {

    private final String marketMakerId;

    private final long uid;

    private final boolean enabled;

    private final List<MarketMakerRiskLimit> riskLimits;
    public MarketMakerProfile(String marketMakerId, long uid, boolean enabled, List<MarketMakerRiskLimit> riskLimits) {
        riskLimits = riskLimits == null ? List.of() : List.copyOf(riskLimits);
    
        this.marketMakerId = marketMakerId;
        this.uid = uid;
        this.enabled = enabled;
        this.riskLimits = riskLimits;
    }

    public Optional<MarketMakerRiskLimit> riskLimit(String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase();
        return riskLimits.stream()
                .filter(limit -> normalized.equalsIgnoreCase(limit.symbol()))
                .findFirst();
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public long uid() {
        return uid;
    }

    public boolean enabled() {
        return enabled;
    }

    public List<MarketMakerRiskLimit> riskLimits() {
        return riskLimits;
    }
}