/*
 * 檔案用途：領域 DTO，回傳 persisted reconciliation report 與 issue 明細。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.entity.ReconciliationReport;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class ReconciliationReportResult {

    private final String id;

    private final Integer schemaVersion;

    private final String reportType;

    private final String status;

    private final String triggeredBy;

    private final Integer scannedAccounts;

    private final Integer issueCount;

    private final Integer errorCount;

    private final Integer warnCount;

    private final String alertRoute;

    private final Instant startedAt;

    private final Instant completedAt;

    private final List<ValidationIssue> issues;
    public ReconciliationReportResult(String id, Integer schemaVersion, String reportType, String status, String triggeredBy, Integer scannedAccounts, Integer issueCount, Integer errorCount, Integer warnCount, String alertRoute, Instant startedAt, Instant completedAt, List<ValidationIssue> issues) {
        this.id = id;
        this.schemaVersion = schemaVersion;
        this.reportType = reportType;
        this.status = status;
        this.triggeredBy = triggeredBy;
        this.scannedAccounts = scannedAccounts;
        this.issueCount = issueCount;
        this.errorCount = errorCount;
        this.warnCount = warnCount;
        this.alertRoute = alertRoute;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.issues = issues;
    }

    public static ReconciliationReportResult from(
            ReconciliationReport report,
            List<ReconciliationReportIssue> issues
    ) {
        return new ReconciliationReportResult(
                report.getId(),
                report.getSchemaVersion(),
                report.getReportType(),
                report.getStatus(),
                report.getTriggeredBy(),
                report.getScannedAccounts(),
                report.getIssueCount(),
                report.getErrorCount(),
                report.getWarnCount(),
                report.getAlertRoute(),
                report.getStartedAt(),
                report.getCompletedAt(),
                issues.stream().map(ReconciliationReportIssue::toValidationIssue).toList()
        );
    }

    public String id() {
        return id;
    }

    public Integer schemaVersion() {
        return schemaVersion;
    }

    public String reportType() {
        return reportType;
    }

    public String status() {
        return status;
    }

    public String triggeredBy() {
        return triggeredBy;
    }

    public Integer scannedAccounts() {
        return scannedAccounts;
    }

    public Integer issueCount() {
        return issueCount;
    }

    public Integer errorCount() {
        return errorCount;
    }

    public Integer warnCount() {
        return warnCount;
    }

    public String alertRoute() {
        return alertRoute;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public List<ValidationIssue> issues() {
        return issues;
    }
}