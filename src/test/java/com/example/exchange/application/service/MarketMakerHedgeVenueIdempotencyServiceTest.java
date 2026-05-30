/*
 * 檔案用途：測試 hedge venue idempotency 營運檢視，確保 uncertain outcome 可被查出。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeOrderResult;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyRecord;
import com.example.exchange.domain.model.dto.HedgeVenueIdempotencyReport;
import com.example.exchange.domain.repository.HedgeVenueIdempotencyStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketMakerHedgeVenueIdempotencyServiceTest {

    @Test
    @DisplayName("unresolved 會列出 pending claim 與 retryable venue result，排除已終結結果")
    void unresolvedReportsPendingAndRetryableVenueOutcomes() {
        MemStore store = new MemStore();
        MarketMakerHedgeVenueIdempotencyService service =
                new MarketMakerHedgeVenueIdempotencyService(store);
        store.records.add(new HedgeVenueIdempotencyRecord("ref-pending", "fp-1", false, null));
        store.records.add(new HedgeVenueIdempotencyRecord(
                "ref-timeout",
                "fp-2",
                true,
                HedgeOrderResult.retryableRejected("HEDGE_VENUE_TIMEOUT")
        ));
        store.records.add(new HedgeVenueIdempotencyRecord(
                "ref-accepted",
                "fp-3",
                true,
                new HedgeOrderResult(true, "venue-1", null, false, Instant.parse("2026-05-28T00:00:00Z"))
        ));

        // 流程：營運報告只顯示仍需人工查 venue 或等待 reconcile 的 effectful submit。
        HedgeVenueIdempotencyReport report = service.unresolved(50);

        assertThat(report.issueCount()).isEqualTo(2);
        assertThat(report.issues()).extracting("refId")
                .containsExactly("ref-pending", "ref-timeout");
        assertThat(report.issues()).extracting("reason")
                .containsExactly("HEDGE_VENUE_SUBMIT_PENDING", "HEDGE_VENUE_TIMEOUT");
        assertThat(report.issues().getFirst().completed()).isFalse();
        assertThat(report.issues().get(1).retryable()).isTrue();
    }

    @Test
    @DisplayName("unresolved 拒絕無界限查詢 limit")
    void unresolvedRejectsInvalidLimit() {
        MarketMakerHedgeVenueIdempotencyService service =
                new MarketMakerHedgeVenueIdempotencyService(new MemStore());

        // 流程：operator unresolved view 只允許 bounded page size，避免 effectful submit 記錄被一次掃完。
        assertThatThrownBy(() -> service.unresolved(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
        assertThatThrownBy(() -> service.unresolved(501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
    }

    private static class MemStore implements HedgeVenueIdempotencyStore {
        private final List<HedgeVenueIdempotencyRecord> records = new ArrayList<>();

        @Override
        public Optional<HedgeVenueIdempotencyRecord> find(String refId) {
            return records.stream()
                    .filter(record -> refId.equals(record.refId()))
                    .findFirst();
        }

        @Override
        public boolean claim(String refId, String fingerprint) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HedgeVenueIdempotencyRecord complete(String refId, String fingerprint, HedgeOrderResult result) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<HedgeVenueIdempotencyRecord> findUnresolved(int limit) {
            return records.stream()
                    .filter(record -> !record.completed()
                            || (record.result() != null && record.result().retryable()))
                    .limit(limit)
                    .toList();
        }
    }
}
