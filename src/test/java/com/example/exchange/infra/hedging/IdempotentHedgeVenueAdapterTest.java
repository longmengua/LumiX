/*
 * 檔案用途：測試 hedge venue submit 的 idempotency envelope。
 */
package com.example.exchange.infra.hedging;

import com.example.exchange.domain.model.dto.HedgeOrderRequest;
import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.service.HedgeVenueAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotentHedgeVenueAdapterTest {

    @Test
    @DisplayName("相同 refId 與 payload 成功後重送不會再次呼叫 venue")
    /**
     * 流程：第一次 submit accepted，第二次用同一 refId/payload 重送。
     * 期望：直接回傳已保存結果，避免 retry 或 client 重送造成外部 venue 重複下單。
     */
    void duplicateAcceptedSubmitReturnsStoredResultWithoutCallingVenueAgain() {
        RecordingVenueAdapter delegate = new RecordingVenueAdapter(List.of(HedgeOrderResult.accepted("venue-1")));
        IdempotentHedgeVenueAdapter adapter = new IdempotentHedgeVenueAdapter(delegate);

        HedgeOrderResult first = adapter.submit(request("hedge-ref-1", "1.000"));
        HedgeOrderResult duplicate = adapter.submit(request("hedge-ref-1", "1.0000"));

        assertThat(first.accepted()).isTrue();
        assertThat(duplicate.accepted()).isTrue();
        assertThat(duplicate.venueOrderId()).isEqualTo("venue-1");
        assertThat(delegate.requests).hasSize(1);
    }

    @Test
    @DisplayName("相同 refId 但 payload 不同時拒絕而不呼叫 venue")
    /**
     * 流程：同一 refId 第一次送 1 BTC，第二次改成 2 BTC。
     * 期望：判定 idempotency key conflict，不能把同一外部 request identity 指向不同 effect。
     */
    void duplicateRefIdWithDifferentPayloadIsRejectedBeforeVenueCall() {
        RecordingVenueAdapter delegate = new RecordingVenueAdapter(List.of(HedgeOrderResult.accepted("venue-1")));
        IdempotentHedgeVenueAdapter adapter = new IdempotentHedgeVenueAdapter(delegate);

        adapter.submit(request("hedge-ref-1", "1.000"));
        HedgeOrderResult conflict = adapter.submit(request("hedge-ref-1", "2.000"));

        assertThat(conflict.accepted()).isFalse();
        assertThat(conflict.retryable()).isFalse();
        assertThat(conflict.reason()).isEqualTo("HEDGE_VENUE_IDEMPOTENCY_CONFLICT");
        assertThat(delegate.requests).hasSize(1);
    }

    @Test
    @DisplayName("第一次結果不確定時 duplicate submit 不會再次呼叫 venue")
    /**
     * 流程：第一次 submit 發生 timeout 類 retryable rejection，遠端可能已收到。
     * 期望：同 refId duplicate 不再送出第二次外部 effect，而是回報 outcome uncertain 交由對帳/人工處理。
     */
    void duplicateAfterRetryableOutcomeIsBlockedAsUncertain() {
        RecordingVenueAdapter delegate = new RecordingVenueAdapter(List.of(HedgeOrderResult.retryableRejected("VENUE_TIMEOUT")));
        IdempotentHedgeVenueAdapter adapter = new IdempotentHedgeVenueAdapter(delegate);

        HedgeOrderResult first = adapter.submit(request("hedge-ref-1", "1.000"));
        HedgeOrderResult duplicate = adapter.submit(request("hedge-ref-1", "1.000"));

        assertThat(first.retryable()).isTrue();
        assertThat(duplicate.accepted()).isFalse();
        assertThat(duplicate.retryable()).isTrue();
        assertThat(duplicate.reason()).isEqualTo("HEDGE_VENUE_OUTCOME_UNCERTAIN");
        assertThat(delegate.requests).hasSize(1);
    }

    @Test
    @DisplayName("venue submit 丟例外後同 refId 重送會被 pending claim 擋下")
    /**
     * 流程：adapter 已 claim refId，但 delegate 在外部結果不明時丟出例外。
     * 期望：下一次同 refId/payload 不再呼叫 venue，回報 uncertain，避免 crash/timeout 後 duplicate external effect。
     */
    void duplicateAfterDelegateExceptionIsBlockedByPendingClaim() {
        ThrowingVenueAdapter delegate = new ThrowingVenueAdapter();
        IdempotentHedgeVenueAdapter adapter = new IdempotentHedgeVenueAdapter(delegate);

        assertThatThrownBy(() -> adapter.submit(request("hedge-ref-1", "1.000")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("network timeout");

        HedgeOrderResult duplicate = adapter.submit(request("hedge-ref-1", "1.000"));

        assertThat(duplicate.accepted()).isFalse();
        assertThat(duplicate.retryable()).isTrue();
        assertThat(duplicate.reason()).isEqualTo("HEDGE_VENUE_OUTCOME_UNCERTAIN");
        assertThat(delegate.calls).isEqualTo(1);
    }


    @Test
    @DisplayName("缺少 refId 的 effectful hedge submit 會被拒絕")
    /**
     * 流程：呼叫 effectful venue submit 但沒有 refId。
     * 期望：直接丟出錯誤，避免沒有 idempotency key 的外部寫入進入 retry path。
     */
    void missingRefIdIsRejectedBeforeVenueCall() {
        RecordingVenueAdapter delegate = new RecordingVenueAdapter(List.of(HedgeOrderResult.accepted("venue-1")));
        IdempotentHedgeVenueAdapter adapter = new IdempotentHedgeVenueAdapter(delegate);

        assertThatThrownBy(() -> adapter.submit(request(null, "1.000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refId");
        assertThat(delegate.requests).isEmpty();
    }

    private static HedgeOrderRequest request(String refId, String quantity) {
        return new HedgeOrderRequest(
                "mm-1",
                9001,
                "BTCUSDT",
                OrderSide.BUY,
                new BigDecimal(quantity),
                new BigDecimal("100.00"),
                new BigDecimal("101.00"),
                refId
        );
    }

    private static class RecordingVenueAdapter implements HedgeVenueAdapter {
        private final List<HedgeOrderResult> results;
        private final List<HedgeOrderRequest> requests = new ArrayList<>();

        private RecordingVenueAdapter(List<HedgeOrderResult> results) {
            this.results = results;
        }

        @Override
        public HedgeOrderResult submit(HedgeOrderRequest request) {
            requests.add(request);
            int index = Math.min(requests.size() - 1, results.size() - 1);
            return results.get(index);
        }
    }

    private static class ThrowingVenueAdapter implements HedgeVenueAdapter {
        private int calls;

        @Override
        public HedgeOrderResult submit(HedgeOrderRequest request) {
            calls++;
            throw new IllegalStateException("network timeout");
        }
    }
}
