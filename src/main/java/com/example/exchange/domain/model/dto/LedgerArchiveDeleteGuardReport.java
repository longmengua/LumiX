/*
 * 檔案用途：ledger hot-path delete guard report，彙總 archive/delete 前不可跳過的驗證。
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
public class LedgerArchiveDeleteGuardReport {

    private final LocalDate reportDate;

    private final boolean approved;

    private final Instant generatedAt;

    private final LedgerArchiveEligibilityReport eligibility;

    private final LedgerArchiveManifest manifest;

    private final LedgerArchiveRestoreSmokeReport restoreSmoke;

    private final LedgerArchiveReplayValidationReport replayValidation;

    private final List<String> blockers;
    public LedgerArchiveDeleteGuardReport(LocalDate reportDate, boolean approved, Instant generatedAt, LedgerArchiveEligibilityReport eligibility, LedgerArchiveManifest manifest, LedgerArchiveRestoreSmokeReport restoreSmoke, LedgerArchiveReplayValidationReport replayValidation, List<String> blockers) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    
        this.reportDate = reportDate;
        this.approved = approved;
        this.generatedAt = generatedAt;
        this.eligibility = eligibility;
        this.manifest = manifest;
        this.restoreSmoke = restoreSmoke;
        this.replayValidation = replayValidation;
        this.blockers = blockers;
    }

    public LocalDate reportDate() {
        return reportDate;
    }

    public boolean approved() {
        return approved;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public LedgerArchiveEligibilityReport eligibility() {
        return eligibility;
    }

    public LedgerArchiveManifest manifest() {
        return manifest;
    }

    public LedgerArchiveRestoreSmokeReport restoreSmoke() {
        return restoreSmoke;
    }

    public LedgerArchiveReplayValidationReport replayValidation() {
        return replayValidation;
    }

    public List<String> blockers() {
        return blockers;
    }
}