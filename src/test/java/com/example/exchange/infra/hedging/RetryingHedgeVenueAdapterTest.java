/*
 * 檔案用途：測試 hedge venue retry decorator。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetryingHedgeVenueAdapterTest {

    @Test
    @DisplayName("submit 會重試 retryable rejection 並回傳後續 accepted 結果")
    void submitRetriesRetryableRejectionUntilAccepted() {
        FlakyVenueAdapter delegate = new FlakyVenueAdapter(
                List.of(
                        HedgeOrderResult.retryableRejected("VENUE_TIMEOUT"),
                        HedgeOrderResult.accepted("venue-1")
                )
        );
        RetryingHedgeVenueAdapter adapter = new RetryingHedgeVenueAdapter(delegate, 3);

        // 流程：第一次 timeout 是暫時錯誤，第二次成功時應停止重試並保留同一個 ref id。
        HedgeOrderResult result = adapter.submit(request());

        assertThat(result.accepted()).isTrue();
        assertThat(result.venueOrderId()).isEqualTo("venue-1");
        assertThat(delegate.requests).hasSize(2)
                .allSatisfy(request -> assertThat(request.refId()).isEqualTo("hedge-ref-1"));
    }

    @Test
    @DisplayName("submit 不會重試 non-retryable rejection")
    void submitDoesNotRetryNonRetryableRejection() {
        FlakyVenueAdapter delegate = new FlakyVenueAdapter(
                List.of(HedgeOrderResult.rejected("INVALID_SYMBOL"))
        );
        RetryingHedgeVenueAdapter adapter = new RetryingHedgeVenueAdapter(delegate, 3);

        // 流程：交易所明確拒絕的 non-retryable 錯誤不能盲目重試。
        HedgeOrderResult result = adapter.submit(request());

        assertThat(result.accepted()).isFalse();
        assertThat(result.retryable()).isFalse();
        assertThat(delegate.requests).hasSize(1);
    }

    @Test
    @DisplayName("submit 會在達到 maxAttempts 後回傳 retry exhausted 結果")
    void submitStopsAfterMaxAttempts() {
        FlakyVenueAdapter delegate = new FlakyVenueAdapter(
                List.of(
                        HedgeOrderResult.retryableRejected("VENUE_TIMEOUT"),
                        HedgeOrderResult.retryableRejected("VENUE_TIMEOUT"),
                        HedgeOrderResult.accepted("too-late")
                )
        );
        RetryingHedgeVenueAdapter adapter = new RetryingHedgeVenueAdapter(delegate, 2);

        // 流程：超過重試上限後仍回傳 retryable rejection，交由上層排程或人工處理。
        HedgeOrderResult result = adapter.submit(request());

        assertThat(result.accepted()).isFalse();
        assertThat(result.retryable()).isTrue();
        assertThat(result.reason()).isEqualTo("VENUE_TIMEOUT");
        assertThat(delegate.requests).hasSize(2);
    }

    @Test
    @DisplayName("submit 會在 retryable rejection 後套用 backoff")
    void submitSleepsWithBackoffBetweenRetryableAttempts() {
        FlakyVenueAdapter delegate = new FlakyVenueAdapter(
                List.of(
                        HedgeOrderResult.retryableRejected("VENUE_TIMEOUT"),
                        HedgeOrderResult.retryableRejected("VENUE_TIMEOUT"),
                        HedgeOrderResult.accepted("venue-1")
                )
        );
        List<Duration> sleeps = new ArrayList<>();
        RetryingHedgeVenueAdapter adapter = new RetryingHedgeVenueAdapter(
                delegate,
                3,
                RetryBackoff.linear(Duration.ofMillis(25)),
                sleeps::add
        );

        // 流程：前兩次是 retryable 錯誤，第三次成功；backoff 應只發生在重試前。
        HedgeOrderResult result = adapter.submit(request());

        assertThat(result.accepted()).isTrue();
        assertThat(sleeps).containsExactly(Duration.ofMillis(25), Duration.ofMillis(50));
        assertThat(delegate.requests).hasSize(3);
    }

    private static HedgeOrderRequest request() {
        return new HedgeOrderRequest(
                "mm-1",
                9001,
                "BTCUSDT",
                OrderSide.BUY,
                new BigDecimal("1.000"),
                new BigDecimal("100.00"),
                new BigDecimal("101.00"),
                "hedge-ref-1"
        );
    }

    private static class FlakyVenueAdapter implements HedgeVenueAdapter {
        private final List<HedgeOrderResult> results;
        private final List<HedgeOrderRequest> requests = new ArrayList<>();

        private FlakyVenueAdapter(List<HedgeOrderResult> results) {
            this.results = results;
        }

        @Override
        public HedgeOrderResult submit(HedgeOrderRequest request) {
            requests.add(request);
            int index = Math.min(requests.size() - 1, results.size() - 1);
            return results.get(index);
        }
    }
}
