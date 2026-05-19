/*
 * 檔案用途：JPA entity，保存 reconciliation report summary。
 */
package com.example.exchange.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "reconciliation_reports",
        indexes = {
                @Index(name = "idx_reconciliation_reports_status", columnList = "status,completed_at"),
                @Index(name = "idx_reconciliation_reports_trigger", columnList = "triggered_by,started_at")
        }
)
public class ReconciliationReport {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "report_type", nullable = false, length = 64)
    private String reportType;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "triggered_by", nullable = false, length = 32)
    private String triggeredBy;

    @Column(name = "scanned_accounts", nullable = false)
    private Integer scannedAccounts;

    @Column(name = "issue_count", nullable = false)
    private Integer issueCount;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "warn_count", nullable = false)
    private Integer warnCount;

    @Column(name = "alert_route", length = 128)
    private String alertRoute;

    @Column(name = "started_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant startedAt;

    @Column(name = "completed_at", nullable = false, columnDefinition = "DATETIME(6)")
    private Instant completedAt;

    public static ReconciliationReport create(
            int schemaVersion,
            String reportType,
            String triggeredBy,
            int scannedAccounts,
            int issueCount,
            int errorCount,
            int warnCount,
            String alertRoute,
            Instant startedAt,
            Instant completedAt
    ) {
        ReconciliationReport report = new ReconciliationReport();
        report.setId(UUID.randomUUID().toString());
        report.setSchemaVersion(schemaVersion);
        report.setReportType(reportType);
        report.setTriggeredBy(triggeredBy);
        report.setScannedAccounts(scannedAccounts);
        report.setIssueCount(issueCount);
        report.setErrorCount(errorCount);
        report.setWarnCount(warnCount);
        report.setStatus(errorCount > 0 ? "FAILED" : issueCount > 0 ? "WARN" : "PASSED");
        report.setAlertRoute(alertRoute);
        report.setStartedAt(startedAt);
        report.setCompletedAt(completedAt);
        return report;
    }
}
