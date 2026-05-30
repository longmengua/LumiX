/*
 * 檔案用途：評估 ledger hot-table archive/delete 前置條件，不直接刪資料。
 */
package com.example.exchange.application.service;

import com.example.exchange.domain.model.dto.FinanceDailyReport;
import com.example.exchange.domain.model.dto.LedgerArchiveEligibilityReport;
import com.example.exchange.domain.model.dto.LedgerTamperEvidenceReport;
import com.example.exchange.domain.repository.WalletLedgerJournal;
import com.example.exchange.infra.config.LedgerArchiveProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerArchiveEligibilityService {

    private final WalletLedgerJournal ledgerJournal;
    private final FinanceReportService financeReportService;
    private final WalletLedgerReplayService walletLedgerReplayService;
    private final LedgerArchiveProperties properties;
    private final Clock clock = Clock.systemUTC();

    public LedgerArchiveEligibilityReport evaluate(LocalDate reportDate) {
        LocalDate date = reportDate == null ? LocalDate.now(clock).minusDays(1) : reportDate;
        Instant cutoff = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<String> blockers = new ArrayList<>();
        long retainedHotDays = Math.max(0, properties.getHotRetentionDays());
        Instant oldestDeletable = Instant.now(clock).minusSeconds(retainedHotDays * 86_400L);
        if (!cutoff.isBefore(oldestDeletable)) {
            blockers.add("RETENTION_WINDOW_NOT_CLOSED");
        }

        long candidateEntryCount = ledgerJournal.findByCreatedAtBetween(
                date.atStartOfDay().toInstant(ZoneOffset.UTC),
                cutoff
        ).size();

        if (properties.isRequireTamperEvidenceClean()) {
            LedgerTamperEvidenceReport tamper = walletLedgerReplayService.verifyTamperEvidence();
            if (tamper.issueCount() > 0) {
                blockers.add("LEDGER_TAMPER_EVIDENCE_ISSUES:" + tamper.issueCount());
            }
        }

        if (properties.isRequireBalancedDailyReport()) {
            FinanceDailyReport dailyReport = financeReportService.dailyReport(date);
            if (!dailyReport.balanced()) {
                blockers.add("DAILY_REPORT_UNBALANCED");
            }
        }

        if (candidateEntryCount == 0) {
            blockers.add("NO_LEDGER_ENTRIES_FOR_DATE");
        }

        return new LedgerArchiveEligibilityReport(
                date,
                cutoff,
                retainedHotDays,
                candidateEntryCount,
                blockers.isEmpty(),
                Instant.now(clock),
                blockers
        );
    }
}
