/*
 * 檔案用途：ledger archive manifest DTO，記錄 export 批次校驗資訊。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;

public record LedgerArchiveManifest(
        String archiveBatchId,
        String dataFamily,
        int schemaVersion,
        LocalDate reportDate,
        Instant fromInclusive,
        Instant toExclusive,
        long sourceEntryCount,
        long sourcePostingCount,
        String aggregateChecksum,
        boolean deleteEligible,
        String restoreInstructions,
        Instant generatedAt
) {
    public LedgerArchiveManifest {
        dataFamily = dataFamily == null || dataFamily.isBlank() ? "wallet_ledger" : dataFamily;
        restoreInstructions = restoreInstructions == null || restoreInstructions.isBlank()
                ? "restore to staging wallet ledger tables, verify row counts and aggregate checksum, then promote"
                : restoreInstructions;
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }
}
