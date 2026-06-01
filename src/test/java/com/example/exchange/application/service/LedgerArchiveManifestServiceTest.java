/*
 * 檔案用途：測試 ledger archive manifest row counts 與 checksum。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.LedgerArchiveDeleteGuardReport;
import com.example.exchange.domain.model.dto.LedgerArchiveManifest;
import com.example.exchange.domain.model.dto.LedgerArchiveReplayValidationReport;
import com.example.exchange.domain.model.dto.LedgerArchiveRestoreSmokeReport;
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

class LedgerArchiveManifestServiceTest {

    @Test
    @DisplayName("generate 會產生 ledger archive manifest 的 row count、checksum 與 delete eligibility")
    void generateBuildsLedgerArchiveManifest() {
        MemLedgerJournal journal = new MemLedgerJournal();
        journal.entries.add(entry("2024-01-01T00:00:00Z"));
        LedgerArchiveProperties properties = new LedgerArchiveProperties();
        properties.setHotRetentionDays(1);
        FinanceReportService financeReportService = new FinanceReportService(journal);
        WalletLedgerReplayService replayService = new WalletLedgerReplayService(journal, new EmptyAccountRepository());
        LedgerArchiveEligibilityService eligibilityService =
                new LedgerArchiveEligibilityService(journal, financeReportService, replayService, properties);
        LedgerArchiveManifestService service =
                new LedgerArchiveManifestService(journal, eligibilityService, financeReportService);

        // 場景：manifest 必須固定 archive batch id、來源筆數、posting 筆數與 aggregate checksum，供刪除前校驗。
        LedgerArchiveManifest manifest = service.generate(LocalDate.parse("2024-01-01"));

        assertThat(manifest.archiveBatchId()).startsWith("ledger-2024-01-01-");
        assertThat(manifest.sourceEntryCount()).isEqualTo(1);
        assertThat(manifest.sourcePostingCount()).isEqualTo(2);
        assertThat(manifest.aggregateChecksum()).hasSize(64);
        assertThat(manifest.deleteEligible()).isTrue();
    }

    @Test
    @DisplayName("restoreSmoke 重新計算 archive manifest row count 與 checksum")
    void restoreSmokeVerifiesManifestCountsAndChecksum() {
        MemLedgerJournal journal = new MemLedgerJournal();
        journal.entries.add(entry("2024-01-01T00:00:00Z"));
        LedgerArchiveProperties properties = new LedgerArchiveProperties();
        properties.setHotRetentionDays(1);
        FinanceReportService financeReportService = new FinanceReportService(journal);
        WalletLedgerReplayService replayService = new WalletLedgerReplayService(journal, new EmptyAccountRepository());
        LedgerArchiveEligibilityService eligibilityService =
                new LedgerArchiveEligibilityService(journal, financeReportService, replayService, properties);
        LedgerArchiveManifestService service =
                new LedgerArchiveManifestService(journal, eligibilityService, financeReportService);
        LedgerArchiveManifest manifest = service.generate(LocalDate.parse("2024-01-01"));

        // 場景：archive restore 前先用 staging 匯入結果重算 count/checksum；這裡用 hot journal 模擬 staging。
        LedgerArchiveRestoreSmokeReport report = service.restoreSmoke(LocalDate.parse("2024-01-01"), manifest);

        assertThat(report.passed()).isTrue();
        assertThat(report.expectedEntryCount()).isEqualTo(1);
        assertThat(report.actualEntryCount()).isEqualTo(1);
        assertThat(report.expectedChecksum()).isEqualTo(report.actualChecksum());
    }

    @Test
    @DisplayName("validateReplayRange 逐日檢查 archived ledger 日報平衡與 restore smoke")
    void validateReplayRangeChecksDailyBalanceAndRestoreSmoke() {
        MemLedgerJournal journal = new MemLedgerJournal();
        journal.entries.add(entry("2024-01-01T00:00:00Z"));
        journal.entries.add(entry("2024-01-02T00:00:00Z"));
        LedgerArchiveProperties properties = new LedgerArchiveProperties();
        properties.setHotRetentionDays(1);
        FinanceReportService financeReportService = new FinanceReportService(journal);
        WalletLedgerReplayService replayService = new WalletLedgerReplayService(journal, new EmptyAccountRepository());
        LedgerArchiveEligibilityService eligibilityService =
                new LedgerArchiveEligibilityService(journal, financeReportService, replayService, properties);
        LedgerArchiveManifestService service =
                new LedgerArchiveManifestService(journal, eligibilityService, financeReportService);

        // 場景：archive 日期區間 replay validation 會逐日驗證日報平衡與 manifest 可還原性。
        LedgerArchiveReplayValidationReport report = service.validateReplayRange(
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-02")
        );

        assertThat(report.passed()).isTrue();
        assertThat(report.daysChecked()).isEqualTo(2);
        assertThat(report.balancedDays()).isEqualTo(2);
        assertThat(report.restoreSmokePassedDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("deleteGuard 只有 eligibility、manifest、restore smoke 與 replay validation 全通過才核准 hot-path delete")
    void deleteGuardRequiresAllArchiveAndReplayChecks() {
        MemLedgerJournal journal = new MemLedgerJournal();
        journal.entries.add(entry("2024-01-01T00:00:00Z"));
        LedgerArchiveProperties properties = new LedgerArchiveProperties();
        properties.setHotRetentionDays(1);
        FinanceReportService financeReportService = new FinanceReportService(journal);
        WalletLedgerReplayService replayService = new WalletLedgerReplayService(journal, new EmptyAccountRepository());
        LedgerArchiveEligibilityService eligibilityService =
                new LedgerArchiveEligibilityService(journal, financeReportService, replayService, properties);
        LedgerArchiveManifestService service =
                new LedgerArchiveManifestService(journal, eligibilityService, financeReportService);

        // 場景：delete guard 是 hot ledger 刪除前最後一道不可跳過的 immutable archive gate。
        LedgerArchiveDeleteGuardReport report = service.deleteGuard(LocalDate.parse("2024-01-01"));

        assertThat(report.approved()).isTrue();
        assertThat(report.eligibility().deleteEligible()).isTrue();
        assertThat(report.manifest().deleteEligible()).isTrue();
        assertThat(report.restoreSmoke().passed()).isTrue();
        assertThat(report.replayValidation().passed()).isTrue();
        assertThat(report.blockers()).isEmpty();
    }

    @Test
    @DisplayName("deleteGuard 會阻擋未通過 retention/restore/replay gate 的 hot-path delete")
    void deleteGuardBlocksUnsafeDelete() {
        MemLedgerJournal journal = new MemLedgerJournal();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        journal.entries.add(WalletLedgerEntry.builder()
                .uid(92)
                .asset("USDT")
                .reason("deposit")
                .amount(new BigDecimal("100"))
                .balanceAfter(new BigDecimal("100"))
                .createdAt(today.atStartOfDay().toInstant(ZoneOffset.UTC))
                .postings(List.of(new WalletLedgerPosting("USER_AVAILABLE", "USDT", new BigDecimal("100"), BigDecimal.ZERO)))
                .build());
        LedgerArchiveProperties properties = new LedgerArchiveProperties();
        properties.setHotRetentionDays(365);
        FinanceReportService financeReportService = new FinanceReportService(journal);
        WalletLedgerReplayService replayService = new WalletLedgerReplayService(journal, new EmptyAccountRepository());
        LedgerArchiveEligibilityService eligibilityService =
                new LedgerArchiveEligibilityService(journal, financeReportService, replayService, properties);
        LedgerArchiveManifestService service =
                new LedgerArchiveManifestService(journal, eligibilityService, financeReportService);

        LedgerArchiveDeleteGuardReport report = service.deleteGuard(today);

        assertThat(report.approved()).isFalse();
        assertThat(report.blockers()).anyMatch(blocker -> blocker.contains("DELETE_ELIGIBILITY_FAILED"));
        assertThat(report.blockers()).anyMatch(blocker -> blocker.contains("REPLAY_VALIDATION_FAILED"));
    }

    private static WalletLedgerEntry entry(String createdAt) {
        return WalletLedgerEntry.builder()
                .uid(92)
                .asset("USDT")
                .reason("deposit")
                .amount(new BigDecimal("100"))
                .balanceAfter(new BigDecimal("100"))
                .createdAt(Instant.parse(createdAt))
                .postings(List.of(
                        new WalletLedgerPosting("USER_AVAILABLE", "USDT", new BigDecimal("100"), BigDecimal.ZERO),
                        new WalletLedgerPosting("EXTERNAL_CASH", "USDT", BigDecimal.ZERO, new BigDecimal("100"))
                ))
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
