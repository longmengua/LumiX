/*
 * 檔案用途：測試 ledger archive/delete eligibility 前置條件。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.LedgerArchiveEligibilityReport;
import com.example.exchange.domain.model.entity.Account;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.AccountRepository;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import com.example.exchange.infra.config.LedgerArchiveProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerArchiveEligibilityServiceTest {

    @Test
    @DisplayName("evaluate 在 retention 已關閉且日報平衡時允許 ledger archive delete eligibility")
    void evaluateAllowsEligibleClosedBalancedLedgerDate() {
        MemLedgerJournal journal = new MemLedgerJournal();
        journal.entries.add(entry("2024-01-01T00:00:00Z", List.of(
                new WalletLedgerPosting("USER_AVAILABLE", "USDT", new BigDecimal("100"), BigDecimal.ZERO),
                new WalletLedgerPosting("EXTERNAL_CASH", "USDT", BigDecimal.ZERO, new BigDecimal("100"))
        )));
        LedgerArchiveProperties properties = new LedgerArchiveProperties();
        properties.setHotRetentionDays(1);
        LedgerArchiveEligibilityService service = service(journal, properties);

        // 場景：舊日期、hash-chain clean、daily finance report balanced，才允許 hot-path delete eligibility。
        LedgerArchiveEligibilityReport report = service.evaluate(LocalDate.parse("2024-01-01"));

        assertThat(report.candidateEntryCount()).isEqualTo(1);
        assertThat(report.deleteEligible()).isTrue();
        assertThat(report.blockers()).isEmpty();
    }

    @Test
    @DisplayName("evaluate 會阻擋 retention 未關閉或日報不平衡的 ledger archive delete")
    void evaluateBlocksWhenRetentionOrBalanceChecksFail() {
        MemLedgerJournal journal = new MemLedgerJournal();
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        journal.entries.add(entry(todayUtc.atStartOfDay().plusHours(1).toInstant(ZoneOffset.UTC).toString(), List.of(
                new WalletLedgerPosting("USER_AVAILABLE", "USDT", new BigDecimal("100"), BigDecimal.ZERO)
        )));
        LedgerArchiveProperties properties = new LedgerArchiveProperties();
        properties.setHotRetentionDays(365);
        LedgerArchiveEligibilityService service = service(journal, properties);

        // 場景：UTC 今天的資料還在 hot retention window 內，且 posting 不平衡，不能刪 hot path。
        LedgerArchiveEligibilityReport report = service.evaluate(todayUtc);

        assertThat(report.deleteEligible()).isFalse();
        assertThat(report.blockers()).contains("RETENTION_WINDOW_NOT_CLOSED", "DAILY_REPORT_UNBALANCED");
    }

    private static LedgerArchiveEligibilityService service(MemLedgerJournal journal, LedgerArchiveProperties properties) {
        FinanceReportService financeReportService = new FinanceReportService(journal);
        WalletLedgerReplayService replayService = new WalletLedgerReplayService(journal, new EmptyAccountRepository());
        return new LedgerArchiveEligibilityService(journal, financeReportService, replayService, properties);
    }

    private static WalletLedgerEntry entry(String createdAt, List<WalletLedgerPosting> postings) {
        return WalletLedgerEntry.builder()
                .uid(91)
                .asset("USDT")
                .reason("deposit")
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
            return List.of();
        }

        @Override
        public List<WalletLedgerEntry> findByCreatedAtBetween(Instant fromInclusive, Instant toExclusive) {
            return entries.stream()
                    .filter(entry -> !entry.getCreatedAt().isBefore(fromInclusive))
                    .filter(entry -> entry.getCreatedAt().isBefore(toExclusive))
                    .toList();
        }
    }

    private static class EmptyAccountRepository implements AccountRepository {
        @Override
        public Optional<Account> findByUid(long uid) {
            return Optional.empty();
        }

        @Override
        public void save(Account account) {
        }
    }
}
