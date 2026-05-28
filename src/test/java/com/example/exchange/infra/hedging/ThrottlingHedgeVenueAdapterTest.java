/*
 * 檔案用途：測試 hedge venue throttle decorator。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ThrottlingHedgeVenueAdapterTest {

    @Test
    @DisplayName("submit 會在連續送單太快時等待最小間隔")
    void submitSleepsWhenRequestsArriveBeforeMinInterval() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-29T00:00:00Z"));
        List<Duration> sleeps = new ArrayList<>();
        RecordingVenueAdapter delegate = new RecordingVenueAdapter();
        ThrottlingHedgeVenueAdapter adapter = new ThrottlingHedgeVenueAdapter(
                delegate,
                Duration.ofMillis(100),
                clock,
                duration -> {
                    sleeps.add(duration);
                    clock.advance(duration);
                }
        );

        // 流程：第一筆立即送出；第二筆距離上一筆只有 40ms，所以要補等 60ms。
        adapter.submit(request("ref-1"));
        clock.advance(Duration.ofMillis(40));
        adapter.submit(request("ref-2"));

        assertThat(sleeps).containsExactly(Duration.ofMillis(60));
        assertThat(delegate.requests).extracting(HedgeOrderRequest::refId)
                .containsExactly("ref-1", "ref-2");
    }

    @Test
    @DisplayName("submit 在已超過最小間隔時不等待")
    void submitDoesNotSleepAfterMinIntervalPassed() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-29T00:00:00Z"));
        List<Duration> sleeps = new ArrayList<>();
        ThrottlingHedgeVenueAdapter adapter = new ThrottlingHedgeVenueAdapter(
                new RecordingVenueAdapter(),
                Duration.ofMillis(100),
                clock,
                sleeps::add
        );

        // 流程：第二筆已晚於 min interval，decorator 不應額外等待。
        adapter.submit(request("ref-1"));
        clock.advance(Duration.ofMillis(150));
        adapter.submit(request("ref-2"));

        assertThat(sleeps).isEmpty();
    }

    private static HedgeOrderRequest request(String refId) {
        return new HedgeOrderRequest(
                "mm-1",
                9001,
                "BTCUSDT",
                OrderSide.BUY,
                new BigDecimal("1.000"),
                new BigDecimal("100.00"),
                new BigDecimal("101.00"),
                refId
        );
    }

    private static final class RecordingVenueAdapter implements HedgeVenueAdapter {
        private final List<HedgeOrderRequest> requests = new ArrayList<>();

        @Override
        public HedgeOrderResult submit(HedgeOrderRequest request) {
            requests.add(request);
            return HedgeOrderResult.accepted("venue-" + requests.size());
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
