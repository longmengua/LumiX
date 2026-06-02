/*
 * 檔案用途：領域 DTO，於服務、事件與介面層之間傳遞結構化資料。
 */
package com.example.exchange.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Gamma API Market DTO。
 *
 * 對應：
 * GET https://gamma-api.polymarket.com/markets
 *
 * 注意：
 * 這是外部 API 回傳物件，不是 JPA Entity。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PredictionGammaMarketDto {

    /**
     * Gamma market id。
     *
     * 有些情況下可作為 conditionId fallback。
     */
    private String id;

    /**
     * Polymarket condition id。
     *
     * 如果 Gamma 回傳有 conditionId，優先用這個。
     */
    private String conditionId;

    /**
     * Market 問題。
     *
     * 例如：
     * Will Mexico win on 2026-06-11?
     */
    private String question;

    /**
     * Outcome market slug。
     *
     * 例如：
     * fifwc-mex-rsa-2026-06-11-mex
     * fifwc-mex-rsa-2026-06-11-draw
     * fifwc-mex-rsa-2026-06-11-rsa
     */
    private String slug;

    /**
     * 是否 active。
     */
    private Boolean active;

    /**
     * 是否 closed。
     */
    private Boolean closed;

    /**
     * 是否接受下單。
     */
    private Boolean acceptingOrders;

    /**
     * 是否啟用 order book。
     */
    private Boolean enableOrderBook;

    /**
     * 是否為 Polymarket negative-risk market。
     */
    private Boolean negRisk;

    /**
     * Market 結束時間。
     */
    private String endDate;

    /**
     * Polymarket 分組項目名稱。
     *
     * TS classifyOutcome 有用到 groupItemTitle。
     */
    private String groupItemTitle;

    /**
     * Polymarket sports group item threshold。
     *
     * FIFA moneyline:
     * - 0 = home win
     * - 1 = draw
     * - 2 = away win
     */
    private String groupItemThreshold;

    /**
     * 運動市場類型。
     */
    private String sportsMarketType;

    /**
     * Gamma 原始 outcomes。
     *
     * 通常是 stringified JSON array。
     */
    private String outcomes;

    /**
     * Gamma 原始 outcomePrices。
     *
     * 通常是 stringified JSON array。
     *
     * 例如：
     * ["0.645","0.355"]
     */
    private String outcomePrices;

    /**
     * Gamma 原始 clobTokenIds。
     *
     * 通常是 stringified JSON array。
     */
    private String clobTokenIds;

    /**
     * 最新成交價。
     */
    private Double lastTradePrice;

    /**
     * 最佳買價。
     *
     * 前端 sellPrice 可以用它。
     */
    private Double bestBid;

    /**
     * 最佳賣價。
     *
     * 前端 buyPrice 可以用它。
     */
    private Double bestAsk;

    /**
     * 流動性。
     *
     * TS pickBest 會根據 liquidityNum 選最好的 market。
     */
    private Double liquidityNum;

    /**
     * 總成交量。
     */
    private Double volumeNum;

    /**
     * 24 小時成交量。
     */
    private Double volume24hr;

    /**
     * Gamma event 列表。
     *
     * 你要用 events[0].slug 或 events[].slug 去 match World Cup fixture。
     */
    private List<PredictionGammaEventDto> events;
}
