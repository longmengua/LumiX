/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
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
        String yesTokenId,
        BigDecimal yesBuyPrice,
        BigDecimal yesSellPrice,
        String noTokenId,
        BigDecimal noBuyPrice,
        BigDecimal noSellPrice,
        BigDecimal probabilityPct,
        BigDecimal liquidity,
        BigDecimal volume,
        BigDecimal volume24hr
) {
}