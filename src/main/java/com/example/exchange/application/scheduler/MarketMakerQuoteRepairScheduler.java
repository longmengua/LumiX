/*
 * 檔案用途：排程入口，定期修復做市商 active quote state 與 open order 的差異。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.MarketMakerQuoteReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketMakerQuoteRepairScheduler {

    private final MarketMakerQuoteReconciliationService quoteReconciliationService;

    @Value("${market-maker.quote-repair.enabled:false}")
    private boolean enabled;

    @Value("${market-maker.quote-repair.limit:50}")
    private int limit;

    @Scheduled(fixedDelayString = "${market-maker.quote-repair.fixed-delay-ms:300000}")
    public void repairActiveQuotes() {
        if (!enabled) {
            return;
        }
        // Repair intentionally fails closed: incomplete quote state is deactivated so the next quote command rebuilds both sides.
        quoteReconciliationService.repairActiveQuotes(limit);
    }
}
