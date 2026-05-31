/*
 * 檔案用途：產生 ledger archive manifest，供 archive-first / verify-checksum / delete-last 流程使用。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.LedgerArchiveEligibilityReport;
import com.example.exchange.domain.model.dto.LedgerArchiveManifest;
import com.example.exchange.domain.model.dto.LedgerArchiveReplayValidationReport;
import com.example.exchange.domain.model.dto.LedgerArchiveRestoreSmokeReport;
import com.example.exchange.domain.model.entity.WalletLedgerEntry;
import com.example.exchange.domain.model.entity.WalletLedgerPosting;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerArchiveManifestService {

    private static final int SCHEMA_VERSION = 1;

    private final WalletLedgerJournal ledgerJournal;
    private final LedgerArchiveEligibilityService eligibilityService;
    private final FinanceReportService financeReportService;

    public LedgerArchiveManifest generate(LocalDate reportDate) {
        LocalDate date = reportDate == null ? LocalDate.now(ZoneOffset.UTC).minusDays(1) : reportDate;
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<WalletLedgerEntry> entries = ledgerJournal.findByCreatedAtBetween(from, to);
        LedgerArchiveEligibilityReport eligibility = eligibilityService.evaluate(date);
        long postingCount = entries.stream()
                .mapToLong(entry -> entry.getPostings() == null ? 0 : entry.getPostings().size())
                .sum();
        String checksum = checksum(entries);
        return new LedgerArchiveManifest(
                "ledger-" + date + "-" + checksum.substring(0, 16),
                "wallet_ledger",
                SCHEMA_VERSION,
                date,
                from,
                to,
                entries.size(),
                postingCount,
                checksum,
                eligibility.deleteEligible(),
                null,
                Instant.now()
        );
    }

    public LedgerArchiveRestoreSmokeReport restoreSmoke(LocalDate reportDate) {
        LedgerArchiveManifest manifest = generate(reportDate);
        return restoreSmoke(reportDate, manifest);
    }

    public LedgerArchiveRestoreSmokeReport restoreSmoke(LocalDate reportDate, LedgerArchiveManifest manifest) {
        LocalDate date = reportDate == null ? LocalDate.now(ZoneOffset.UTC).minusDays(1) : reportDate;
        Instant from = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<WalletLedgerEntry> entries = ledgerJournal.findByCreatedAtBetween(from, to);
        long actualPostingCount = entries.stream()
                .mapToLong(entry -> entry.getPostings() == null ? 0 : entry.getPostings().size())
                .sum();
        String actualChecksum = checksum(entries);
        List<String> blockers = new ArrayList<>();
        if (manifest == null) {
            blockers.add("MISSING_MANIFEST");
        } else {
            if (manifest.sourceEntryCount() != entries.size()) {
                blockers.add("ENTRY_COUNT_MISMATCH");
            }
            if (manifest.sourcePostingCount() != actualPostingCount) {
                blockers.add("POSTING_COUNT_MISMATCH");
            }
            if (!actualChecksum.equals(manifest.aggregateChecksum())) {
                blockers.add("CHECKSUM_MISMATCH");
            }
        }
        return new LedgerArchiveRestoreSmokeReport(
                date,
                manifest == null ? null : manifest.archiveBatchId(),
                manifest == null ? 0 : manifest.sourceEntryCount(),
                entries.size(),
                manifest == null ? 0 : manifest.sourcePostingCount(),
                actualPostingCount,
                manifest == null ? null : manifest.aggregateChecksum(),
                actualChecksum,
                blockers.isEmpty(),
                Instant.now(),
                blockers
        );
    }

    public LedgerArchiveReplayValidationReport validateReplayRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate == null ? LocalDate.now(ZoneOffset.UTC).minusDays(1) : fromDate;
        LocalDate to = toDate == null ? from : toDate;
        if (to.isBefore(from)) {
            return new LedgerArchiveReplayValidationReport(
                    from,
                    to,
                    0,
                    0,
                    0,
                    false,
                    Instant.now(),
                    List.of("INVALID_DATE_RANGE"),
                    List.of()
            );
        }
        List<String> blockers = new ArrayList<>();
        List<LedgerArchiveRestoreSmokeReport> smokeReports = new ArrayList<>();
        int days = 0;
        int balancedDays = 0;
        int smokePassedDays = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days++;
            if (financeReportService.dailyReport(date).balanced()) {
                balancedDays++;
            } else {
                blockers.add("DAILY_REPORT_UNBALANCED:" + date);
            }
            LedgerArchiveRestoreSmokeReport smoke = restoreSmoke(date);
            smokeReports.add(smoke);
            if (smoke.passed()) {
                smokePassedDays++;
            } else {
                blockers.add("RESTORE_SMOKE_FAILED:" + date + ":" + smoke.blockers());
            }
        }
        return new LedgerArchiveReplayValidationReport(
                from,
                to,
                days,
                balancedDays,
                smokePassedDays,
                blockers.isEmpty(),
                Instant.now(),
                blockers,
                smokeReports
        );
    }

    private static String checksum(List<WalletLedgerEntry> entries) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (WalletLedgerEntry entry : entries) {
                digest.update(canonical(entry).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("hash ledger archive manifest failed", e);
        }
    }

    private static String canonical(WalletLedgerEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append(entry.getId()).append('|')
                .append(entry.getUid()).append('|')
                .append(entry.getAsset()).append('|')
                .append(entry.getReason()).append('|')
                .append(entry.getRefId()).append('|')
                .append(entry.getAmount()).append('|')
                .append(entry.getBalanceAfter()).append('|')
                .append(entry.getCreatedAt());
        for (WalletLedgerPosting posting : entry.getPostings() == null ? List.<WalletLedgerPosting>of() : entry.getPostings()) {
            builder.append('|')
                    .append(posting.accountCode()).append(':')
                    .append(posting.asset()).append(':')
                    .append(posting.debit()).append(':')
                    .append(posting.credit());
        }
        return builder.toString();
    }
}
