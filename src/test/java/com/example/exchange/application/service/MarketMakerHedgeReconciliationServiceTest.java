/*
 * 檔案用途：測試做市商 hedge decision 與 venue fill 對帳。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.HedgeDecisionAuditRecord;
import com.example.exchange.domain.model.dto.HedgeFillRecord;
import com.example.exchange.domain.model.dto.HedgeReconciliationReport;
import com.example.exchange.domain.model.enums.OrderSide;
import com.example.exchange.domain.repository.HedgeDecisionAuditStore;
import com.example.exchange.domain.repository.HedgeFillStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketMakerHedgeReconciliationServiceTest {

    @Test
    @DisplayName("reconcileMarketMaker 會找出缺 fill 與部分成交 hedge decision")
    void reconcileMarketMakerReportsMissingAndUnderfilledHedges() {
        MemDecisionStore decisionStore = new MemDecisionStore();
        MemFillStore fillStore = new MemFillStore();
        MarketMakerHedgeReconciliationService service =
                new MarketMakerHedgeReconciliationService(decisionStore, fillStore);
        decisionStore.append(decision("venue-filled", "100.00", true));
        decisionStore.append(decision("venue-under", "100.00", true));
        decisionStore.append(decision("venue-missing", "100.00", true));
        decisionStore.append(decision("venue-rejected", "100.00", false));
        fillStore.append(fill("venue-filled", "1.000", "100.00"));
        fillStore.append(fill("venue-under", "0.500", "100.00"));

        // 流程：只檢查 accepted decision；完全成交不報 issue，缺 fill/少成交要進報告。
        HedgeReconciliationReport report = service.reconcileMarketMaker("mm-1", 50);

        assertThat(report.checkedDecisions()).isEqualTo(3);
        assertThat(report.issueCount()).isEqualTo(2);
        assertThat(report.issues()).extracting("reason")
                .containsExactly("UNDERFILLED_NOTIONAL", "MISSING_FILL");
        assertThat(report.issues().getFirst().filledNotional()).isEqualByComparingTo("50.00000");
    }

    @Test
    @DisplayName("reconcileMarketMaker 會找出 overfill hedge decision")
    void reconcileMarketMakerReportsOverfilledHedge() {
        MemDecisionStore decisionStore = new MemDecisionStore();
        MemFillStore fillStore = new MemFillStore();
        MarketMakerHedgeReconciliationService service =
                new MarketMakerHedgeReconciliationService(decisionStore, fillStore);
        decisionStore.append(decision("venue-over", "100.00", true));
        fillStore.append(fill("venue-over", "1.200", "100.00"));

        // 流程：venue fill 名目大於 hedge decision 名目時，標成 overfill 供營運處理。
        HedgeReconciliationReport report = service.reconcileMarketMaker("mm-1", 50);

        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues().getFirst().reason()).isEqualTo("OVERFILLED_NOTIONAL");
        assertThat(report.issues().getFirst().filledNotional()).isEqualByComparingTo("120.00000");
    }

    @Test
    @DisplayName("reconcileMarketMaker 會找出 hedge trade ref 與 ledger ref 缺口")
    void reconcileMarketMakerReportsTradeAndLedgerReferenceGaps() {
        MemDecisionStore decisionStore = new MemDecisionStore();
        MemFillStore fillStore = new MemFillStore();
        MarketMakerHedgeReconciliationService service =
                new MarketMakerHedgeReconciliationService(decisionStore, fillStore);
        decisionStore.append(decision("venue-missing-trade", "100.00", true, null));
        decisionStore.append(decision("venue-missing-ledger", "100.00", true, "trade-ref-2"));
        decisionStore.append(decision("venue-mismatch", "100.00", true, "trade-ref-3"));
        fillStore.append(fill("venue-missing-trade", "1.000", "100.00", "trade-ref-1", "ledger-ref-1"));
        fillStore.append(fill("venue-missing-ledger", "1.000", "100.00", "trade-ref-2", null));
        fillStore.append(fill("venue-mismatch", "1.000", "100.00", "wrong-trade-ref", "ledger-ref-3"));

        // 流程：notional 對得上時，仍要能報出 trade ref 與 ledger ref 缺口，供 fee/PnL 對帳追查。
        HedgeReconciliationReport report = service.reconcileMarketMaker("mm-1", 50);

        assertThat(report.issueCount()).isEqualTo(3);
        assertThat(report.issues()).extracting("reason")
                .containsExactly(
                        "MISSING_INTERNAL_TRADE_REF",
                        "MISSING_LEDGER_REF",
                        "TRADE_LEDGER_REF_MISMATCH"
                );
        assertThat(report.issues().get(1).ledgerRefId()).isNull();
        assertThat(report.issues().get(2).internalTradeRefId()).isEqualTo("trade-ref-3");
    }

    @Test
    @DisplayName("reconcileMarketMaker 拒絕無界限查詢 limit")
    void reconcileMarketMakerRejectsInvalidLimit() {
        MarketMakerHedgeReconciliationService service =
                new MarketMakerHedgeReconciliationService(new MemDecisionStore(), new MemFillStore());

        // 流程：reconciliation 報告要有固定頁面大小上限，避免單次後台請求掃過量 decision audit。
        assertThatThrownBy(() -> service.reconcileMarketMaker("mm-1", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
        assertThatThrownBy(() -> service.reconcileMarketMaker("mm-1", 501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 500");
    }

    private static HedgeDecisionAuditRecord decision(String venueOrderId, String notional, boolean accepted) {
        return decision(venueOrderId, notional, accepted, "trade-ref-1");
    }

    private static HedgeDecisionAuditRecord decision(
            String venueOrderId,
            String notional,
            boolean accepted,
            String internalTradeRefId
    ) {
        return new HedgeDecisionAuditRecord(
                null,
                "mm-1",
                "BTCUSDT",
                accepted,
                accepted ? "ACCEPTED" : "KILL_SWITCH_ENABLED",
                new BigDecimal(notional),
                venueOrderId,
                "trade-ref-1",
                internalTradeRefId,
                Instant.parse("2026-05-28T00:00:00Z"),
                null
        );
    }

    private static HedgeFillRecord fill(String venueOrderId, String quantity, String price) {
        return fill(venueOrderId, quantity, price, "trade-ref-1", "ledger-ref-1");
    }

    private static HedgeFillRecord fill(
            String venueOrderId,
            String quantity,
            String price,
            String refId,
            String ledgerRefId
    ) {
        return new HedgeFillRecord(
                null,
                "mm-1",
                "BTCUSDT",
                venueOrderId,
                "fill-" + venueOrderId,
                OrderSide.BUY,
                new BigDecimal(quantity),
                new BigDecimal(price),
                BigDecimal.ZERO,
                "USDT",
                refId,
                ledgerRefId,
                Instant.parse("2026-05-28T00:00:01Z"),
                null
        );
    }

    private static class MemDecisionStore implements HedgeDecisionAuditStore {
        private final List<HedgeDecisionAuditRecord> records = new ArrayList<>();

        @Override
        public void append(HedgeDecisionAuditRecord record) {
            records.add(record);
        }

        @Override
        public List<HedgeDecisionAuditRecord> findByMarketMakerId(String marketMakerId, int limit) {
            return records.stream()
                    .filter(record -> marketMakerId.equals(record.marketMakerId()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<HedgeDecisionAuditRecord> findByRefId(String refId) {
            return records.stream()
                    .filter(record -> refId.equals(record.refId()))
                    .toList();
        }
    }

    private static class MemFillStore implements HedgeFillStore {
        private final List<HedgeFillRecord> records = new ArrayList<>();

        @Override
        public void append(HedgeFillRecord record) {
            records.add(record);
        }

        @Override
        public Optional<HedgeFillRecord> findByVenueOrderIdAndVenueFillId(String venueOrderId, String venueFillId) {
            return records.stream()
                    .filter(record -> venueOrderId.equals(record.venueOrderId())
                            && venueFillId.equals(record.venueFillId()))
                    .findFirst();
        }

        @Override
        public List<HedgeFillRecord> findByMarketMakerId(String marketMakerId, int limit) {
            return records.stream()
                    .filter(record -> marketMakerId.equals(record.marketMakerId()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<HedgeFillRecord> findByVenueOrderId(String venueOrderId) {
            return records.stream()
                    .filter(record -> venueOrderId.equals(record.venueOrderId()))
                    .toList();
        }

        @Override
        public List<HedgeFillRecord> findByRefId(String refId) {
            return records.stream()
                    .filter(record -> refId.equals(record.refId()))
                    .toList();
        }
    }
}
