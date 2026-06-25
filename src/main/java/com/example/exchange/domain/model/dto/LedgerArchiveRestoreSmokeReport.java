/*
 * 檔案用途：ledger archive manifest restore smoke DTO，檢查 row count 與 checksum 是否可還原。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class LedgerArchiveRestoreSmokeReport {

    private final LocalDate reportDate;

    private final String archiveBatchId;

    private final long expectedEntryCount;

    private final long actualEntryCount;

    private final long expectedPostingCount;

    private final long actualPostingCount;

    private final String expectedChecksum;

    private final String actualChecksum;

    private final boolean passed;

    private final Instant generatedAt;

    private final List<String> blockers;
    public LedgerArchiveRestoreSmokeReport(LocalDate reportDate, String archiveBatchId, long expectedEntryCount, long actualEntryCount, long expectedPostingCount, long actualPostingCount, String expectedChecksum, String actualChecksum, boolean passed, Instant generatedAt, List<String> blockers) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    
        this.reportDate = reportDate;
        this.archiveBatchId = archiveBatchId;
        this.expectedEntryCount = expectedEntryCount;
        this.actualEntryCount = actualEntryCount;
        this.expectedPostingCount = expectedPostingCount;
        this.actualPostingCount = actualPostingCount;
        this.expectedChecksum = expectedChecksum;
        this.actualChecksum = actualChecksum;
        this.passed = passed;
        this.generatedAt = generatedAt;
        this.blockers = blockers;
    }

    public LocalDate reportDate() {
        return reportDate;
    }

    public String archiveBatchId() {
        return archiveBatchId;
    }

    public long expectedEntryCount() {
        return expectedEntryCount;
    }

    public long actualEntryCount() {
        return actualEntryCount;
    }

    public long expectedPostingCount() {
        return expectedPostingCount;
    }

    public long actualPostingCount() {
        return actualPostingCount;
    }

    public String expectedChecksum() {
        return expectedChecksum;
    }

    public String actualChecksum() {
        return actualChecksum;
    }

    public boolean passed() {
        return passed;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<String> blockers() {
        return blockers;
    }
}