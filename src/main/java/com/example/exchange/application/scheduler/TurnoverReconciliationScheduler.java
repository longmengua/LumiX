/*
 * 檔案用途：排程入口，定期批次對帳 turnover read model、trade tape 與 ledger ref。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.TurnoverReconciliationService;
import com.example.exchange.infra.config.TurnoverReconciliationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TurnoverReconciliationScheduler {

    private final TurnoverReconciliationService reconciliationService;
    private final TurnoverReconciliationProperties properties;

    @Scheduled(fixedDelayString = "${turnover.reconciliation.fixed-delay-ms:300000}")
    public void reconcileRecentTurnover() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant upper = Instant.now();
        Instant lower = upper.minusSeconds(Math.max(1, properties.getLookbackSeconds()));
        reconciliationService.reconcileRecent(lower, upper, properties.getBatchLimit());
    }
}
