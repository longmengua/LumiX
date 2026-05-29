/*
 * 檔案用途：應用層排程任務，定期清理高流量 market-data history。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.MarketDataRetentionService;
import com.example.exchange.infra.config.MarketDataRetentionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MarketDataRetentionScheduler {

    private final MarketDataRetentionService retentionService;
    private final MarketDataRetentionProperties properties;

    @Scheduled(fixedDelayString = "${market-data.retention.fixed-delay-ms:300000}")
    public void purgeExpiredHistory() {
        if (!properties.isEnabled()) return;
        retentionService.purgeExpired(Instant.now());
    }
}
