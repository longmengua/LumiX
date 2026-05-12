package com.example.exchange.application.scheduler;

import com.example.exchange.domain.service.PolymarketSyncService;
import com.example.exchange.domain.service.PolymarketPriceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prediction Market 排程。
 *
 * 注意：
 * 這裡不做 discovery。
 *
 * discovery 是重任務，只允許手動 API：
 * POST /api/prediction/markets/discover
 *
 * Scheduler 只做：
 * 1. 價格刷新
 * 2. 已知 key 補同步
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionMarketScheduler {

    private final PolymarketPriceService priceRefreshService;

    private final PolymarketSyncService fullSyncService;

    private final AtomicBoolean priceRunning = new AtomicBoolean(false);

    private final AtomicBoolean keySyncRunning = new AtomicBoolean(false);

    /**
     * 每 5 秒刷新一次價格。
     *
     * application.yml 可設定：
     *
     * polymarket:
     *   price-refresh-ms: 5000
     */
    @Scheduled(fixedDelayString = "${polymarket.price-refresh-ms:10000}")
    public void refreshPrices() {
        if (!priceRunning.compareAndSet(false, true)) {
            log.warn("Prediction price refresh skipped, previous job still running");
            return;
        }

        try {
            priceRefreshService.refreshPrices();
        } catch (Exception e) {
            log.warn("Prediction price refresh scheduler failed", e);
        } finally {
            priceRunning.set(false);
        }
    }

    /**
     * 每 10 分鐘同步一次已知 key。
     *
     * 注意：
     * 這裡不是 full discovery。
     *
     * 它只會讀 prediction_market_sync_key，
     * 然後同步已知 event 的 outcome markets。
     *
     * application.yml 可設定：
     *
     * polymarket:
     *   key-sync-ms: 600000
     */
    @Scheduled(fixedDelayString = "${polymarket.key-sync-ms:600000}")
    public void syncKnownKeys() {
        if (!keySyncRunning.compareAndSet(false, true)) {
            log.warn("Prediction key sync skipped, previous job still running");
            return;
        }

        try {
            fullSyncService.syncResume();
        } catch (Exception e) {
            log.warn("Prediction key sync scheduler failed", e);
        } finally {
            keySyncRunning.set(false);
        }
    }
}