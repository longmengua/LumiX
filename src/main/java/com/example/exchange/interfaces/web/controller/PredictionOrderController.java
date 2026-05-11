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
 * Prediction Market API Controller
 *
 * Base Path:
 * /api/prediction
 *
 * 用途：
 * 1. 查詢市場列表
 * 2. Quote
 * 3. 下單
 * 4. 手動同步 Gamma markets
 * 5. 手動刷新價格
 * 6. 查詢同步進度
 */
@RestController
@RequestMapping("/api/prediction")
@RequiredArgsConstructor
public class PredictionOrderController {
    private final PredictionMarketDiscoveryService predictionMarketDiscoveryService;

    /**
     * Market 查詢服務。
     *
     * 給前端 Bitmart 類 UI 使用。
     */
    private final PredictionMarketService predictionMarketService;

    /**
     * Full Sync Service。
     *
     * 現在用途：
     * 1. Gamma 全量拉取 active/open markets
     * 2. 用 market.events[].slug match eventSlug
     * 3. 分類 homeWin / draw / awayWin
     * 4. 寫入 prediction_market_info
     * 5. 更新 sync progress
     *
     * 不再依賴 /markets/slug/{eventSlug}
     * 不再依賴 CLOB price sync。
     */
    private final PredictionMarketFullSyncService predictionMarketFullSyncService;

    /**
     * Price Refresh Service。
     *
     * 用途：
     * 只刷新已存在 market 的價格欄位：
     * - bestBid
     * - bestAsk
     * - lastTradePrice
     * - outcomePrices
     * - liquidity
     * - volume
     * - volume24hr
     */
    private final PredictionMarketPriceRefreshService predictionMarketPriceRefreshService;

    /**
     * 手動同步 markets。
     *
     * HTTP:
     * POST /api/prediction/markets/sync
     *
     * 行為：
     * - resume 模式
     * - 從上次 lastSyncKeyId 繼續
     * - 保留 sync-progress 查詢能力
     */
    @PostMapping("/markets/sync")
    public String syncMarkets() {
        return predictionMarketFullSyncService.syncResume();
    }

    /**
     * 重置進度後重新全量同步。
     *
     * HTTP:
     * POST /api/prediction/markets/sync-reset
     */
    @PostMapping("/markets/sync-reset")
    public String resetAndSyncMarkets() {
        return predictionMarketFullSyncService.resetAndSync();
    }

    /**
     * 查詢同步進度。
     *
     * HTTP:
     * GET /api/prediction/markets/sync-progress
     */
    @GetMapping("/markets/sync-progress")
    public Object syncProgress() {
        return predictionMarketFullSyncService.getProgress();
    }

    /**
     * 手動刷新價格。
     *
     * HTTP:
     * POST /api/prediction/markets/price-refresh
     *
     * 用途：
     * - 不重新 match metadata
     * - 只更新 Gamma price 欄位
     */
    @PostMapping("/markets/price-refresh")
    public String refreshPrices() {
        predictionMarketPriceRefreshService.refreshPrices();
        return "Prediction market price refresh triggered";
    }

    /**
     * 查詢 Prediction Markets。
     *
     * HTTP:
     * GET /api/prediction/markets
     */
    @GetMapping("/markets")
    public List<PredictionMarketResponse> getMarkets() {
        return predictionMarketService.getMarkets();
    }

    /**
     * 手動全量 discovery。
     *
     * 用途：
     * 1. 全量拉 Gamma active/open markets
     * 2. 自動發現 FIFA World Cup events
     * 3. 自動建立 prediction_market_sync_key
     * 4. 自動建立 prediction_market_info
     *
     * 注意：
     * 這是重任務，不建議高頻呼叫。
     */
    @PostMapping("/markets/discover")
    public String discoverMarkets() {
        return predictionMarketDiscoveryService.discoverWorldCupMarkets();
    }
}