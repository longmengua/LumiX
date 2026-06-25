/*
 * 檔案用途：ledger archive manifest DTO，記錄 export 批次校驗資訊。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class LedgerArchiveManifest {

    private final String archiveBatchId;

    private final String dataFamily;

    private final int schemaVersion;

    private final LocalDate reportDate;

    private final Instant fromInclusive;

    private final Instant toExclusive;

    private final long sourceEntryCount;

    private final long sourcePostingCount;

    private final String aggregateChecksum;

    private final boolean deleteEligible;

    private final String restoreInstructions;

    private final Instant generatedAt;
    public LedgerArchiveManifest(String archiveBatchId, String dataFamily, int schemaVersion, LocalDate reportDate, Instant fromInclusive, Instant toExclusive, long sourceEntryCount, long sourcePostingCount, String aggregateChecksum, boolean deleteEligible, String restoreInstructions, Instant generatedAt) {
        dataFamily = dataFamily == null || dataFamily.isBlank() ? "wallet_ledger" : dataFamily;
        restoreInstructions = restoreInstructions == null || restoreInstructions.isBlank()
                ? "restore to staging wallet ledger tables, verify row counts and aggregate checksum, then promote"
                : restoreInstructions;
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    
        this.archiveBatchId = archiveBatchId;
        this.dataFamily = dataFamily;
        this.schemaVersion = schemaVersion;
        this.reportDate = reportDate;
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.sourceEntryCount = sourceEntryCount;
        this.sourcePostingCount = sourcePostingCount;
        this.aggregateChecksum = aggregateChecksum;
        this.deleteEligible = deleteEligible;
        this.restoreInstructions = restoreInstructions;
        this.generatedAt = generatedAt;
    }

    public String archiveBatchId() {
        return archiveBatchId;
    }

    public String dataFamily() {
        return dataFamily;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public LocalDate reportDate() {
        return reportDate;
    }

    public Instant fromInclusive() {
        return fromInclusive;
    }

    public Instant toExclusive() {
        return toExclusive;
    }

    public long sourceEntryCount() {
        return sourceEntryCount;
    }

    public long sourcePostingCount() {
        return sourcePostingCount;
    }

    public String aggregateChecksum() {
        return aggregateChecksum;
    }

    public boolean deleteEligible() {
        return deleteEligible;
    }

    public String restoreInstructions() {
        return restoreInstructions;
    }

    public Instant generatedAt() {
        return generatedAt;
    }
}