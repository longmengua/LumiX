/*
 * 檔案用途：ledger archive manifest restore smoke DTO，檢查 row count 與 checksum 是否可還原。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LedgerArchiveRestoreSmokeReport(
        LocalDate reportDate,
        String archiveBatchId,
        long expectedEntryCount,
        long actualEntryCount,
        long expectedPostingCount,
        long actualPostingCount,
        String expectedChecksum,
        String actualChecksum,
        boolean passed,
        Instant generatedAt,
        List<String> blockers
) {
    public LedgerArchiveRestoreSmokeReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
