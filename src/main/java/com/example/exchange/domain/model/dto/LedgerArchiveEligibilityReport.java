/*
 * 檔案用途：ledger archive/delete eligibility 報告，落實刪除前置條件。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LedgerArchiveEligibilityReport(
        LocalDate reportDate,
        Instant cutoffExclusive,
        long retainedHotDays,
        long candidateEntryCount,
        boolean deleteEligible,
        Instant generatedAt,
        List<String> blockers
) {
    public LedgerArchiveEligibilityReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
