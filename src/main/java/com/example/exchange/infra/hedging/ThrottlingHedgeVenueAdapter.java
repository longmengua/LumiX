/*
 * 檔案用途：Hedge venue adapter decorator，限制連續送單間隔。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.service.HedgeVenueAdapter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class ThrottlingHedgeVenueAdapter implements HedgeVenueAdapter {

    private final HedgeVenueAdapter delegate;
    private final Duration minInterval;
    private final Clock clock;
    private final Sleeper sleeper;
    private Instant lastSubmittedAt;

    public ThrottlingHedgeVenueAdapter(HedgeVenueAdapter delegate, Duration minInterval) {
        this(delegate, minInterval, Clock.systemUTC(), Sleeper.threadSleeper());
    }

    public ThrottlingHedgeVenueAdapter(
            HedgeVenueAdapter delegate,
            Duration minInterval,
            Clock clock,
            Sleeper sleeper
    ) {
        this.delegate = delegate;
        this.minInterval = minInterval == null ? Duration.ZERO : minInterval;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.sleeper = sleeper == null ? Sleeper.threadSleeper() : sleeper;
    }

    @Override
    public synchronized HedgeOrderResult submit(HedgeOrderRequest request) {
        throttleIfNeeded();
        HedgeOrderResult result = delegate.submit(request);
        lastSubmittedAt = Instant.now(clock);
        return result;
    }

    private void throttleIfNeeded() {
        if (lastSubmittedAt == null || minInterval.isZero() || minInterval.isNegative()) {
            return;
        }
        Instant nextAllowedAt = lastSubmittedAt.plus(minInterval);
        Instant now = Instant.now(clock);
        if (now.isBefore(nextAllowedAt)) {
            sleeper.sleep(Duration.between(now, nextAllowedAt));
        }
    }
}
