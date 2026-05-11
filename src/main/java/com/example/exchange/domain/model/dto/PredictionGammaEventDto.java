package com.example.exchange.domain.model.dto;

import lombok.Getter;
import lombok.Setter;

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
}