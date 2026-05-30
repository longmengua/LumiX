/*
 * 檔案用途：測試 durable ledger 財務日報彙總。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.FinanceDailyReport;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceReportServiceTest {

    @Test
    @DisplayName("dailyReport 依 reason/asset/accountCode 彙總 debit credit 並檢查平衡")
    void dailyReportAggregatesLedgerPostingsByReasonAssetAndAccount() {
        MemLedgerJournal journal = new MemLedgerJournal();
        journal.entries.add(entry(
                1,
                "USDT",
                "trade_fee",
                "2026-05-30T01:00:00Z",
                List.of(
                        new WalletLedgerPosting("USER_CASH", "USDT", BigDecimal.ZERO, new BigDecimal("2.50")),
                        new WalletLedgerPosting("FEE_INCOME", "USDT", new BigDecimal("2.50"), BigDecimal.ZERO)
                )
        ));
        journal.entries.add(entry(
                2,
                "USDT",
                "bonus_credit_consume",
                "2026-05-30T02:00:00Z",
                List.of(
                        new WalletLedgerPosting("USER_BONUS_AVAILABLE", "USDT", BigDecimal.ZERO, new BigDecimal("1.00")),
                        new WalletLedgerPosting("USER_FEE_EXPENSE", "USDT", new BigDecimal("1.00"), BigDecimal.ZERO)
                )
        ));
        journal.entries.add(entry(
                3,
                "USDT",
                "trade_fee",
                "2026-05-31T01:00:00Z",
                List.of(
                        new WalletLedgerPosting("USER_CASH", "USDT", BigDecimal.ZERO, new BigDecimal("9.99")),
                        new WalletLedgerPosting("FEE_INCOME", "USDT", new BigDecimal("9.99"), BigDecimal.ZERO)
                )
        ));
        FinanceReportService service = new FinanceReportService(journal);

        // 場景：2026-05-30 的日報只納入 UTC 當天 entries，並輸出財務可檢查的借貸平衡。
        FinanceDailyReport report = service.dailyReport(LocalDate.parse("2026-05-30"));

        assertThat(report.entryCount()).isEqualTo(2);
        assertThat(report.totalDebit()).isEqualByComparingTo("3.50");
        assertThat(report.totalCredit()).isEqualByComparingTo("3.50");
        assertThat(report.balanced()).isTrue();
        assertThat(report.lines()).hasSize(4);
        assertThat(report.lines())
                .anySatisfy(line -> {
                    assertThat(line.reason()).isEqualTo("trade_fee");
                    assertThat(line.accountCode()).isEqualTo("FEE_INCOME");
                    assertThat(line.debit()).isEqualByComparingTo("2.50");
                    assertThat(line.credit()).isZero();
                });
    }

    private static WalletLedgerEntry entry(
            long uid,
            String asset,
            String reason,
            String createdAt,
            List<WalletLedgerPosting> postings
    ) {
        return WalletLedgerEntry.builder()
                .uid(uid)
                .asset(asset)
                .reason(reason)
                .amount(BigDecimal.ONE)
                .balanceAfter(BigDecimal.ZERO)
                .createdAt(Instant.parse(createdAt))
                .postings(postings)
                .build();
    }

    private static class MemLedgerJournal implements WalletLedgerJournal {
        private final List<WalletLedgerEntry> entries = new ArrayList<>();

        @Override
        public void append(WalletLedgerEntry entry) {
            entries.add(entry);
        }

        @Override
        public List<WalletLedgerEntry> findByUid(long uid) {
            return entries.stream().filter(entry -> entry.getUid() == uid).toList();
        }

        @Override
        public List<WalletLedgerEntry> findByUidAndAsset(long uid, String asset) {
            return entries.stream()
                    .filter(entry -> entry.getUid() == uid)
                    .filter(entry -> asset.equals(entry.getAsset()))
                    .toList();
        }

        @Override
        public List<WalletLedgerEntry> findByRefId(String refId) {
            return entries.stream().filter(entry -> refId.equals(entry.getRefId())).toList();
        }

        @Override
        public List<WalletLedgerEntry> findByCreatedAtBetween(Instant fromInclusive, Instant toExclusive) {
            return entries.stream()
                    .filter(entry -> !entry.getCreatedAt().isBefore(fromInclusive))
                    .filter(entry -> entry.getCreatedAt().isBefore(toExclusive))
                    .toList();
        }
    }
}
