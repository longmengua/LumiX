package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.service.PredictionMarketDiscoveryService;
import com.example.exchange.domain.service.PredictionMarketFullSyncService;
import com.example.exchange.domain.service.PredictionMarketPriceRefreshService;
import com.example.exchange.domain.service.PredictionMarketService;
import com.example.exchange.interfaces.web.dto.PredictionMarketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prediction Market API Controller。
 *
 * 目前只保留 market 相關功能：
 *
 * 1. Discovery：
 *    全量拉 Gamma，發現世界杯 markets。
 *
 * 2. Sync：
 *    根據已知 sync key，用 search 補齊缺失 outcome。
 *
 * 3. Price Refresh：
 *    根據已知 marketSlug 更新價格。
 *
 * 4. Query：
 *    給前端 Bitmart 類 UI 查詢市場資料。
 */
@RestController
@RequestMapping("/api/prediction")
@RequiredArgsConstructor
public class PredictionOrderController {

    /**
     * 前端查詢 market list。
     */
    private final PredictionMarketService predictionMarketService;

    /**
     * 全量 discovery service。
     *
     * 重任務，手動觸發。
     */
    private final PredictionMarketDiscoveryService predictionMarketDiscoveryService;

    /**
     * key sync service。
     *
     * 用 search 補齊已知 key 的 outcome。
     */
    private final PredictionMarketFullSyncService predictionMarketFullSyncService;

    /**
     * 價格刷新 service。
     */
    private final PredictionMarketPriceRefreshService predictionMarketPriceRefreshService;

    /**
     * 全量發現世界杯 markets。
     *
     * 這個 API 會：
     * 1. Gamma 全量拉 active/open markets
     * 2. 過濾 FIFA World Cup
     * 3. 自動建立 prediction_market_sync_key
     * 4. 自動建立 prediction_market_info
     *
     * HTTP:
     * POST /api/prediction/markets/discover
     */
    @PostMapping("/markets/discover")
    public String discoverMarkets() {
        return predictionMarketDiscoveryService.discoverWorldCupMarkets();
    }

    /**
     * 補同步已知 key。
     *
     * 這個 API 不做全量拉取。
     * 它會針對 prediction_market_sync_key 裡的資料，
     * 用 teamA + teamB search Gamma，
     * 嘗試補齊 homeWin / draw / awayWin。
     *
     * HTTP:
     * POST /api/prediction/markets/sync
     */
    @PostMapping("/markets/sync")
    public String syncMarkets() {
        return predictionMarketFullSyncService.syncResume();
    }

    /**
     * 重置 sync progress 後重新同步所有 key。
     *
     * HTTP:
     * POST /api/prediction/markets/sync-reset
     */
    @PostMapping("/markets/sync-reset")
    public String resetAndSyncMarkets() {
        return predictionMarketFullSyncService.resetAndSync();
    }

    /**
     * 查詢 sync progress。
     *
     * HTTP:
     * GET /api/prediction/markets/sync-progress
     */
    @GetMapping("/markets/sync-progress")
    public Object syncProgress() {
        return predictionMarketFullSyncService.getProgress();
    }

    /**
     * 指定重試某一場 event。
     *
     * 這個 API 會重新 search Gamma，
     * 嘗試補齊指定 eventSlug 的 outcome。
     *
     * HTTP:
     * POST /api/prediction/markets/retry/{eventSlug}
     */
    @PostMapping("/markets/retry/{eventSlug}")
    public String retryMarket(
            @PathVariable String eventSlug
    ) {
        return predictionMarketFullSyncService.retryEvent(eventSlug);
    }

    /**
     * 手動刷新價格。
     *
     * 這個 API 只更新已知 marketSlug 的價格：
     * - bestBid
     * - bestAsk
     * - lastTradePrice
     * - liquidity
     * - volume
     *
     * HTTP:
     * POST /api/prediction/markets/price-refresh
     */
    @PostMapping("/markets/price-refresh")
    public String refreshPrices() {
        predictionMarketPriceRefreshService.refreshPrices();
        return "Prediction market price refresh triggered";
    }

    /**
     * 查詢 Prediction Markets。
     *
     * 給前端 Bitmart 類 UI 使用。
     *
     * HTTP:
     * GET /api/prediction/markets
     */
    @GetMapping("/markets")
    public List<PredictionMarketResponse> getMarkets() {
        return predictionMarketService.getMarkets();
    }
}