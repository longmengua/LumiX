package com.example.exchange.interfaces.web.dto;

import java.math.BigDecimal;

/**
 * 前端 outcome response。
 *
 * 一筆代表：
 * - homeWin
 * - draw
 * - awayWin
 */
public record PredictionOutcomeResponse(
        String outcomeKey,
        String label,
        String question,
        String marketSlug,
        BigDecimal buyPrice,
        BigDecimal sellPrice,
        BigDecimal probabilityPct,
        BigDecimal lastTradePrice,
        BigDecimal liquidity,
        BigDecimal volume,
        BigDecimal volume24hr
) {
}