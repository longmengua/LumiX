/*
 * 檔案用途：Turnover batch 對帳報告 DTO，彙總排程或營運手動觸發結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record TurnoverReconciliationBatchReport(
        Instant fromInclusive,
        Instant toExclusive,
        int sampledRecordCount,
        int matchReportCount,
        int issueCount,
        Instant generatedAt,
        List<TurnoverReconciliationReport> reports
) {
    public TurnoverReconciliationBatchReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        reports = reports == null ? List.of() : List.copyOf(reports);
    }
}
