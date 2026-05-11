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