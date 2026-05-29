/*
 * 檔案用途：Spring runtime hook，在應用 ready 後啟動 configured matching worker symbols。
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.MatchingWorkerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingWorkerStartupListener {

    private final MatchingWorkerProperties properties;
    private final MatchingWorkerLifecycleService lifecycleService;

    @EventListener(ApplicationReadyEvent.class)
    public void startConfiguredSymbolsWhenEnabled() {
        if (!properties.isEnabled()) {
            return;
        }
        // Lifecycle service 會依序 acquire lease、執行 recovery，只有 validation valid 才保存 readiness。
        lifecycleService.startConfiguredSymbols();
    }
}
