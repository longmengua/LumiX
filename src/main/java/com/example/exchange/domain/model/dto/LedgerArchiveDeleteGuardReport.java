/*
 * 檔案用途：ledger hot-path delete guard report，彙總 archive/delete 前不可跳過的驗證。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LedgerArchiveDeleteGuardReport(
        LocalDate reportDate,
        boolean approved,
        Instant generatedAt,
        LedgerArchiveEligibilityReport eligibility,
        LedgerArchiveManifest manifest,
        LedgerArchiveRestoreSmokeReport restoreSmoke,
        LedgerArchiveReplayValidationReport replayValidation,
        List<String> blockers
) {
    public LedgerArchiveDeleteGuardReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
