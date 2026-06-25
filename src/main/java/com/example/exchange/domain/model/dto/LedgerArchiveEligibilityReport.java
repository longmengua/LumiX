/*
 * 檔案用途：ledger archive/delete eligibility 報告，落實刪除前置條件。
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
public class LedgerArchiveEligibilityReport {

    private final LocalDate reportDate;

    private final Instant cutoffExclusive;

    private final long retainedHotDays;

    private final long candidateEntryCount;

    private final boolean deleteEligible;

    private final Instant generatedAt;

    private final List<String> blockers;
    public LedgerArchiveEligibilityReport(LocalDate reportDate, Instant cutoffExclusive, long retainedHotDays, long candidateEntryCount, boolean deleteEligible, Instant generatedAt, List<String> blockers) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    
        this.reportDate = reportDate;
        this.cutoffExclusive = cutoffExclusive;
        this.retainedHotDays = retainedHotDays;
        this.candidateEntryCount = candidateEntryCount;
        this.deleteEligible = deleteEligible;
        this.generatedAt = generatedAt;
        this.blockers = blockers;
    }

    public LocalDate reportDate() {
        return reportDate;
    }

    public Instant cutoffExclusive() {
        return cutoffExclusive;
    }

    public long retainedHotDays() {
        return retainedHotDays;
    }

    public long candidateEntryCount() {
        return candidateEntryCount;
    }

    public boolean deleteEligible() {
        return deleteEligible;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<String> blockers() {
        return blockers;
    }
}