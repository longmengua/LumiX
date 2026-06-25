/*
 * 檔案用途：ledger archive 日期區間 replay validation DTO，彙總日報平衡與 restore smoke 結果。
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
public class LedgerArchiveReplayValidationReport {

    private final LocalDate fromDate;

    private final LocalDate toDate;

    private final int daysChecked;

    private final int balancedDays;

    private final int restoreSmokePassedDays;

    private final boolean passed;

    private final Instant generatedAt;

    private final List<String> blockers;

    private final List<LedgerArchiveRestoreSmokeReport> restoreSmokeReports;
    public LedgerArchiveReplayValidationReport(LocalDate fromDate, LocalDate toDate, int daysChecked, int balancedDays, int restoreSmokePassedDays, boolean passed, Instant generatedAt, List<String> blockers, List<LedgerArchiveRestoreSmokeReport> restoreSmokeReports) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
        restoreSmokeReports = restoreSmokeReports == null ? List.of() : List.copyOf(restoreSmokeReports);
    
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.daysChecked = daysChecked;
        this.balancedDays = balancedDays;
        this.restoreSmokePassedDays = restoreSmokePassedDays;
        this.passed = passed;
        this.generatedAt = generatedAt;
        this.blockers = blockers;
        this.restoreSmokeReports = restoreSmokeReports;
    }

    public LocalDate fromDate() {
        return fromDate;
    }

    public LocalDate toDate() {
        return toDate;
    }

    public int daysChecked() {
        return daysChecked;
    }

    public int balancedDays() {
        return balancedDays;
    }

    public int restoreSmokePassedDays() {
        return restoreSmokePassedDays;
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

    public List<LedgerArchiveRestoreSmokeReport> restoreSmokeReports() {
        return restoreSmokeReports;
    }
}