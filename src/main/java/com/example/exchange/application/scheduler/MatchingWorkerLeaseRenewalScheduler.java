/*
 * 檔案用途：排程入口，定期為 matching worker 已取得 ownership 的 symbols 續租。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.MatchingWorkerLifecycleService;
import com.example.exchange.infra.config.MatchingWorkerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingWorkerLeaseRenewalScheduler {

    private final MatchingWorkerProperties properties;
    private final MatchingWorkerLifecycleService lifecycleService;

    @Scheduled(fixedDelayString = "${matching-worker.renew-interval-ms:10000}")
    public void renewOwnedSymbols() {
        if (!properties.isEnabled()) {
            return;
        }
        // Lifecycle service 會在續租失敗時移除 readiness，避免 stale owner 繼續接 live command。
        lifecycleService.renewOwnedSymbols();
    }
}
