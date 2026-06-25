/*
 * 檔案用途：Turnover batch 對帳報告 DTO，彙總排程或營運手動觸發結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class TurnoverReconciliationBatchReport {

    private final Instant fromInclusive;

    private final Instant toExclusive;

    private final int sampledRecordCount;

    private final int matchReportCount;

    private final int issueCount;

    private final Instant generatedAt;

    private final List<TurnoverReconciliationReport> reports;
    public TurnoverReconciliationBatchReport(Instant fromInclusive, Instant toExclusive, int sampledRecordCount, int matchReportCount, int issueCount, Instant generatedAt, List<TurnoverReconciliationReport> reports) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        reports = reports == null ? List.of() : List.copyOf(reports);
    
        this.fromInclusive = fromInclusive;
        this.toExclusive = toExclusive;
        this.sampledRecordCount = sampledRecordCount;
        this.matchReportCount = matchReportCount;
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.reports = reports;
    }

    public Instant fromInclusive() {
        return fromInclusive;
    }

    public Instant toExclusive() {
        return toExclusive;
    }

    public int sampledRecordCount() {
        return sampledRecordCount;
    }

    public int matchReportCount() {
        return matchReportCount;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<TurnoverReconciliationReport> reports() {
        return reports;
    }
}