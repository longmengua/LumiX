/*
 * 檔案用途：Spring runtime hook，在 REST app ready 後恢復本機 in-memory matching book。
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.MatchingBookRecoveryProperties;
import com.example.exchange.infra.config.MatchingWorkerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchingBookRecoveryStartupListener {

    private final MatchingBookRecoveryProperties properties;
    private final MatchingWorkerProperties workerProperties;
    private final MatchingBookRecoveryService recoveryService;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverBooksWhenRestAppOwnsRuntimeMatching() {
        if (!properties.isEnabled() || workerProperties.isEnabled()) {
            return;
        }
        // REST-mode MVP still uses the local in-memory engine, so startup must rebuild it before users inspect depth.
        recoveryService.recoverConfiguredBooks();
    }
}
