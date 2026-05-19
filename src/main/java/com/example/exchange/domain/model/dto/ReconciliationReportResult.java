/*
 * 檔案用途：領域 DTO，回傳 persisted reconciliation report 與 issue 明細。
 */
package com.example.exchange.domain.model.dto;

import com.example.exchange.domain.model.entity.ReconciliationReport;
import com.example.exchange.domain.model.entity.ReconciliationReportIssue;

import java.time.Instant;
import java.util.List;

public record ReconciliationReportResult(
        String id,
        Integer schemaVersion,
        String reportType,
        String status,
        String triggeredBy,
        Integer scannedAccounts,
        Integer issueCount,
        Integer errorCount,
        Integer warnCount,
        String alertRoute,
        Instant startedAt,
        Instant completedAt,
        List<ValidationIssue> issues
) {
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
}
