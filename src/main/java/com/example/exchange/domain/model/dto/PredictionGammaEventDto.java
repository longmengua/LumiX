/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Gamma API Event DTO。
 *
 * 注意：
 * event slug 才是你要 match 的世界杯賽事 slug。
 */
@Getter
@Setter
public class PredictionGammaEventDto {

    /**
     * Event slug。
     *
     * 例如：
     * fifwc-mex-rsa-2026-06-11
     */
    private String slug;

    /**
     * Event title。
     *
     * 例如：
     * Mexico vs South Africa
     */
    private String title;

    /**
     * Event 開始時間。
     */
    private String startTime;

    /**
     * Event 日期。
     */
    private String eventDate;

    /**
     * Series slug。
     *
     * 世界杯足球通常是：
     * soccer-fifwc
     */
    private String seriesSlug;

    /**
     * Event 底下的 markets。
     *
     * GET /events 會直接帶回這個欄位，
     * FIFA World Cup discovery 優先使用 events endpoint。
     */
    private List<PredictionGammaMarketDto> markets;
}
