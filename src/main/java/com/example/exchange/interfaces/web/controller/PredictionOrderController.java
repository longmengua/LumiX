package com.example.exchange.interfaces.web.controller;

import com.example.exchange.domain.model.dto.PolymarketPlaceOrderRequest;
import com.example.exchange.domain.model.dto.PolymarketPlaceOrderResponse;
import com.example.exchange.domain.service.PolymarketDiscoveryService;
import com.example.exchange.domain.service.PolymarketMarketService;
import com.example.exchange.domain.service.PolymarketOrderService;
import com.example.exchange.domain.service.PolymarketPriceService;
import com.example.exchange.domain.service.PolymarketSyncService;
import com.example.exchange.interfaces.web.dto.PredictionMarketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prediction Market API Controller。
 *
 * 目前包含：
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
 *
 * 5. Order：
 *    根據前端選擇的 outcome / YES / NO / BUY / SELL，
 *    轉換成 Polymarket CLOB 下單請求。
 */
@RestController
@RequestMapping("/api/prediction")
@RequiredArgsConstructor
public class PredictionOrderController {

    /**
     * 前端查詢 market list。
     */
    private final PolymarketMarketService predictionMarketService;

    /**
     * 全量 discovery service。
     *
     * 重任務，手動觸發。
     */
    private final PolymarketDiscoveryService predictionMarketDiscoveryService;

    /**
     * key sync service。
     *
     * 用 search 補齊已知 key 的 outcome。
     */
    private final PolymarketSyncService predictionMarketFullSyncService;

    /**
     * 價格刷新 service。
     */
    private final PolymarketPriceService predictionMarketPriceRefreshService;

    /**
     * Polymarket 下單 service。
     *
     * 主要流程：
     * 1. 接收前端傳入的 outcome 下單資料
     * 2. 根據 direction 判斷使用 yesTokenId / noTokenId
     * 3. 根據 direction 判斷 BUY / SELL
     * 4. 根據價格與 USDT 金額換算 shares size
     * 5. 建立 CLOB signed order
     * 6. 呼叫 Polymarket CLOB /order
     */
    private final PolymarketOrderService polymarketOrderService;

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

    /**
     * 建立 Polymarket 下單。
     *
     * 這個 API 給前端交易按鈕使用。
     *
     * 前端需要傳：
     * - eventSlug
     * - marketSlug
     * - outcomeKey
     * - yesTokenId
     * - noTokenId
     * - yesBuyPrice
     * - yesSellPrice
     * - noBuyPrice
     * - noSellPrice
     * - direction: BUY_YES / SELL_YES / BUY_NO / SELL_NO
     * - usdtAmount
     * - orderType: 建議先用 FOK
     *
     * 後端會做：
     * 1. 根據 direction 選 tokenId
     * 2. 根據 direction 選 BUY / SELL side
     * 3. 根據 price 換算 shares size
     * 4. 建立 signed CLOB order
     * 5. 呼叫 Polymarket CLOB 下單 API
     *
     * HTTP:
     * POST /api/prediction/orders
     */
    @PostMapping("/orders")
    public PolymarketPlaceOrderResponse placeOrder(
            @Valid @RequestBody PolymarketPlaceOrderRequest request
    ) {
        return polymarketOrderService.placeOrder(request);
    }
}