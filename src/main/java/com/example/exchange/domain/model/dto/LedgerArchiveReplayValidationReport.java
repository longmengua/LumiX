/*
 * 檔案用途：ledger archive 日期區間 replay validation DTO，彙總日報平衡與 restore smoke 結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LedgerArchiveReplayValidationReport(
        LocalDate fromDate,
        LocalDate toDate,
        int daysChecked,
        int balancedDays,
        int restoreSmokePassedDays,
        boolean passed,
        Instant generatedAt,
        List<String> blockers,
        List<LedgerArchiveRestoreSmokeReport> restoreSmokeReports
) {
    public LedgerArchiveReplayValidationReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        restoreSmokeReports = restoreSmokeReports == null ? List.of() : List.copyOf(restoreSmokeReports);
    }
}
