/*
 * 檔案用途：抽象 sleep 行為，讓 retry/backoff 測試不用真的等待。
 */
package com.example.exchange.infra.hedging;

import java.time.Duration;

@FunctionalInterface
public interface Sleeper {

    void sleep(Duration duration);

    static Sleeper threadSleeper() {
        return duration -> {
            try {
                Thread.sleep(Math.max(0, duration.toMillis()));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("sleep interrupted", ex);
            }
        };
    }
}
