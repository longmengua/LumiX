package com.example.exchange.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionMarketScheduler {

    private final PredictionMarketPriceRefreshService priceRefreshService;

    /**
     * 防止上一次還沒跑完，下一次又進來。
     */
    private final AtomicBoolean priceRefreshing = new AtomicBoolean(false);

    /**
     * 每 N 秒刷新一次價格。
     *
     * application.yml:
     *
     * prediction:
     *   price-refresh-fixed-delay-ms: 30000
     */
    @Scheduled(
            fixedDelayString = "${prediction.price-refresh-fixed-delay-ms:30000}"
    )
    public void refreshPredictionPrices() {
        if (!priceRefreshing.compareAndSet(false, true)) {
            log.warn("Prediction price refresh skipped, previous job still running");
            return;
        }

        try {
            priceRefreshService.refreshPrices();
        } catch (Exception e) {
            log.warn("Prediction price refresh failed", e);
        } finally {
            priceRefreshing.set(false);
        }
    }
}