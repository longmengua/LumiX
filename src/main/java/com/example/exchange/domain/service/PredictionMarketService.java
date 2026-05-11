package com.example.exchange.domain.service;

import com.example.exchange.domain.model.entity.PredictionMarketInfoEntity;
import com.example.exchange.domain.model.entity.PredictionMarketSyncKeyEntity;
import com.example.exchange.domain.repository.jpa.PredictionMarketInfoRepository;
import com.example.exchange.domain.repository.jpa.PredictionMarketSyncKeyRepository;
import com.example.exchange.interfaces.web.dto.PredictionMarketResponse;
import com.example.exchange.interfaces.web.dto.PredictionOutcomeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Prediction Market 查詢服務。
 *
 * 用途：
 * 給前端 Bitmart 類 UI 查詢市場列表。
 *
 * 資料來源：
 * 1. prediction_market_sync_key：賽事層資料
 * 2. prediction_market_info：outcome 層資料與價格
 *
 * 不再依賴：
 * prediction_market_stats
 */
@Service
@RequiredArgsConstructor
public class PredictionMarketService {

    private final PredictionMarketSyncKeyRepository syncKeyRepository;
    private final PredictionMarketInfoRepository marketInfoRepository;

    /**
     * 查詢所有啟用中的世界杯 Prediction Markets。
     */
    public List<PredictionMarketResponse> getMarkets() {
        return syncKeyRepository.findBySyncEnabledTrueOrderByIdAsc()
                .stream()
                .map(this::toMarketResponse)
                .toList();
    }

    /**
     * event 層轉 response。
     */
    private PredictionMarketResponse toMarketResponse(
            PredictionMarketSyncKeyEntity key
    ) {
        List<PredictionOutcomeResponse> outcomes =
                marketInfoRepository.findByEventSlug(key.getEventSlug())
                        .stream()
                        .filter(this::isDisplayableOutcome)
                        .sorted(Comparator.comparingInt(this::outcomeOrder))
                        .map(this::toOutcomeResponse)
                        .toList();

        return new PredictionMarketResponse(
                key.getEventSlug(),
                key.getEventTitle(),
                key.getTeamA(),
                key.getTeamB(),
                key.getEventDate() == null ? null : key.getEventDate().toString(),
                outcomes
        );
    }

    /**
     * outcome market 轉前端 response。
     *
     * Bitmart UI 只需要：
     * - outcomeKey
     * - label
     * - buyPrice
     * - sellPrice
     * - probabilityPct
     */
    private PredictionOutcomeResponse toOutcomeResponse(
            PredictionMarketInfoEntity entity
    ) {
        BigDecimal bestAsk = toBigDecimal(entity.getBestAsk());
        BigDecimal bestBid = toBigDecimal(entity.getBestBid());
        BigDecimal staticYesPrice = toBigDecimal(entity.getStaticYesPrice());
        BigDecimal lastTradePrice = toBigDecimal(entity.getLastTradePrice());

        /**
         * 買入價：
         * 優先用 bestAsk。
         * 沒有 bestAsk 就用 outcomePrices[0]。
         */
        BigDecimal buyPrice = firstNonNull(
                bestAsk,
                staticYesPrice,
                lastTradePrice
        );

        /**
         * 賣出價：
         * 優先用 bestBid。
         * 沒有 bestBid 就用 outcomePrices[0]。
         */
        BigDecimal sellPrice = firstNonNull(
                bestBid,
                staticYesPrice,
                lastTradePrice
        );

        /**
         * 機率：
         * MVP 先用 buyPrice * 100。
         */
        BigDecimal probabilityPct =
                buyPrice == null
                        ? BigDecimal.ZERO
                        : buyPrice.multiply(BigDecimal.valueOf(100));

        return new PredictionOutcomeResponse(
                entity.getOutcomeKey(),
                entity.getOutcomeLabel(),
                entity.getQuestion(),
                entity.getMarketSlug(),
                buyPrice,
                sellPrice,
                probabilityPct,
                lastTradePrice,
                toBigDecimal(entity.getLiquidity()),
                toBigDecimal(entity.getVolume()),
                toBigDecimal(entity.getVolume24hr())
        );
    }

    /**
     * 只顯示 Bitmart UI 支援的三種 outcome。
     */
    private boolean isDisplayableOutcome(
            PredictionMarketInfoEntity entity
    ) {
        if (entity.getOutcomeKey() == null) {
            return false;
        }

        return switch (entity.getOutcomeKey()) {
            case "homeWin", "draw", "awayWin" -> true;
            default -> false;
        };
    }

    /**
     * 固定排序：
     * 1. homeWin
     * 2. draw
     * 3. awayWin
     */
    private int outcomeOrder(
            PredictionMarketInfoEntity entity
    ) {
        if (entity.getOutcomeKey() == null) {
            return 99;
        }

        return switch (entity.getOutcomeKey()) {
            case "homeWin" -> 1;
            case "draw" -> 2;
            case "awayWin" -> 3;
            default -> 99;
        };
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private BigDecimal firstNonNull(
            BigDecimal first,
            BigDecimal second,
            BigDecimal third
    ) {
        if (first != null) {
            return first;
        }

        if (second != null) {
            return second;
        }

        return third;
    }
}