/*
 * 檔案用途：做市商自動報價 DTO，供後台顯示 runner 設定與目前啟用狀態。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;

public record MarketMakerAutoQuoteStatus(
        boolean enabled,
        long fixedDelayMs,
        int maxProfilesPerRun,
        BigDecimal quoteQuantity,
        int halfSpreadTicks,
        int ladderLevelsPerSide,
        int pulseTicks,
        String refPrefix
) {
}
