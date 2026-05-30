/*
 * 檔案用途：排程入口，定期執行做市商 inventory-aware hedge execution。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.MarketMakerHedgeExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketMakerHedgeExecutionScheduler {

    private final MarketMakerHedgeExecutionService hedgeExecutionService;

    @Value("${market-maker.hedge-execution.enabled:false}")
    private boolean enabled;

    @Value("${market-maker.hedge-execution.ref-prefix:scheduled}")
    private String refPrefix;

    @Value("${market-maker.hedge-execution.scheduled-approval-token:}")
    private String approvalToken;

    @Scheduled(fixedDelayString = "${market-maker.hedge-execution.fixed-delay-ms:300000}")
    public void executeEnabledMarketMakerHedges() {
        if (!enabled) {
            return;
        }
        // Execution service 會再次檢查全域 halt；production 可用 lock-enabled 防止多 worker 重複送單。
        hedgeExecutionService.executeForEnabledMarketMakers(refPrefix, approvalToken);
    }
}
