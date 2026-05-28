/*
 * 檔案用途：Hedge venue retry backoff 策略。
 */
package com.example.exchange.infra.hedging;

import java.time.Duration;

@FunctionalInterface
public interface RetryBackoff {

    Duration delay(int attempt);

    static RetryBackoff fixed(Duration delay) {
        Duration normalized = delay == null ? Duration.ZERO : delay;
        return attempt -> normalized;
    }

    static RetryBackoff linear(Duration baseDelay) {
        Duration normalized = baseDelay == null ? Duration.ZERO : baseDelay;
        return attempt -> normalized.multipliedBy(Math.max(1, attempt));
    }
}
