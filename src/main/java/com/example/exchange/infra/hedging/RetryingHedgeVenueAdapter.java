/*
 * 檔案用途：Hedge venue adapter decorator，針對 retryable 結果做有限次重試。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueAdapter;

import java.time.Duration;

public class RetryingHedgeVenueAdapter implements HedgeVenueAdapter {

    private final HedgeVenueAdapter delegate;
    private final int maxAttempts;
    private final RetryBackoff backoff;
    private final Sleeper sleeper;

    public RetryingHedgeVenueAdapter(HedgeVenueAdapter delegate, int maxAttempts) {
        this(delegate, maxAttempts, RetryBackoff.fixed(Duration.ZERO), Sleeper.threadSleeper());
    }

    public RetryingHedgeVenueAdapter(
            HedgeVenueAdapter delegate,
            int maxAttempts,
            RetryBackoff backoff,
            Sleeper sleeper
    ) {
        this.delegate = delegate;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoff = backoff == null ? RetryBackoff.fixed(Duration.ZERO) : backoff;
        this.sleeper = sleeper == null ? Sleeper.threadSleeper() : sleeper;
    }

    @Override
    public HedgeOrderResult submit(HedgeOrderRequest request) {
        HedgeOrderResult lastResult = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                lastResult = delegate.submit(request);
            } catch (RuntimeException ex) {
                lastResult = HedgeOrderResult.retryableRejected("HEDGE_VENUE_EXCEPTION");
            }
            if (lastResult.accepted() || !lastResult.retryable()) {
                return lastResult;
            }
            sleepBeforeNextAttempt(attempt);
        }
        return lastResult == null
                ? HedgeOrderResult.retryableRejected("HEDGE_VENUE_RETRY_EXHAUSTED")
                : HedgeOrderResult.retryableRejected(lastResult.reason());
    }

    private void sleepBeforeNextAttempt(int attempt) {
        if (attempt >= maxAttempts) {
            return;
        }
        Duration delay = backoff.delay(attempt);
        if (!delay.isZero() && !delay.isNegative()) {
            sleeper.sleep(delay);
        }
    }
}
