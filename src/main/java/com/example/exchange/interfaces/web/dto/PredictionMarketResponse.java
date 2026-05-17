/*
 * 檔案用途：Web DTO，定義 REST API 的 request 與 response 資料結構。
 */
package com.example.exchange.interfaces.web.dto;

import java.util.List;

/**
 * 前端 market response。
 *
 * 一筆代表一場比賽。
 */
public record PredictionMarketResponse(
        String eventSlug,
        String eventTitle,
        String teamA,
        String teamB,
        String eventDate,
        List<PredictionOutcomeResponse> outcomes
) {
}