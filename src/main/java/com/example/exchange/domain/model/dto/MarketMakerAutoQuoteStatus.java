/*
 * 檔案用途：做市商自動報價 DTO，供後台顯示 runner 設定與目前啟用狀態。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerAutoQuoteStatus {

    private final boolean enabled;

    private final long fixedDelayMs;

    private final int maxProfilesPerRun;

    private final BigDecimal quoteQuantity;

    private final int halfSpreadTicks;

    private final int ladderLevelsPerSide;

    private final int pulseTicks;

    private final String refPrefix;
    public MarketMakerAutoQuoteStatus(boolean enabled, long fixedDelayMs, int maxProfilesPerRun, BigDecimal quoteQuantity, int halfSpreadTicks, int ladderLevelsPerSide, int pulseTicks, String refPrefix) {
        this.enabled = enabled;
        this.fixedDelayMs = fixedDelayMs;
        this.maxProfilesPerRun = maxProfilesPerRun;
        this.quoteQuantity = quoteQuantity;
        this.halfSpreadTicks = halfSpreadTicks;
        this.ladderLevelsPerSide = ladderLevelsPerSide;
        this.pulseTicks = pulseTicks;
        this.refPrefix = refPrefix;
    }

    public boolean enabled() {
        return enabled;
    }

    public long fixedDelayMs() {
        return fixedDelayMs;
    }

    public int maxProfilesPerRun() {
        return maxProfilesPerRun;
    }

    public BigDecimal quoteQuantity() {
        return quoteQuantity;
    }

    public int halfSpreadTicks() {
        return halfSpreadTicks;
    }

    public int ladderLevelsPerSide() {
        return ladderLevelsPerSide;
    }

    public int pulseTicks() {
        return pulseTicks;
    }

    public String refPrefix() {
        return refPrefix;
    }
}