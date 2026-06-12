/*
 * 檔案用途：應用排程 runner，以獨立 lifecycle loop 驅動內建做市商自動報價。
 */
package com.example.exchange.application.scheduler;

import com.example.exchange.application.service.MarketMakerAutoQuoteService;
import com.example.exchange.domain.model.dto.MarketMakerAutoQuoteRunReport;
import com.example.exchange.infra.config.MarketMakerAutoQuoteProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketMakerAutoQuoteRunner implements SmartLifecycle {

    private final MarketMakerAutoQuoteService autoQuoteService;
    private final MarketMakerAutoQuoteProperties properties;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;
    private volatile boolean running;

    @Override
    public void start() {
        if (!properties.isEnabled() || running) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "market-maker-auto-quote");
            thread.setDaemon(true);
            return thread;
        });
        // 這個 runner 不依賴全域 @EnableScheduling，避免意外啟動其他 MVP 階段排程器。
        future = executor.scheduleWithFixedDelay(this::runSafely, 0, properties.getFixedDelayMs(), TimeUnit.MILLISECONDS);
        running = true;
        log.info("market maker auto quote runner started fixedDelayMs={}", properties.getFixedDelayMs());
    }

    @Override
    public void stop() {
        if (future != null) {
            future.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        running = false;
        log.info("market maker auto quote runner stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    private void runSafely() {
        try {
            MarketMakerAutoQuoteRunReport report = autoQuoteService.runOnce();
            if (report.placedCount() > 0) {
                log.debug("market maker auto quote placed={} skipped={} sequence={}",
                        report.placedCount(), report.skippedCount(), report.sequence());
            }
        } catch (Exception ex) {
            // 背景做市 loop 單輪失敗不能殺掉 thread；下一輪可因 order book/profile 狀態恢復而成功。
            log.warn("market maker auto quote run failed: {}", ex.getMessage(), ex);
        }
    }
}
