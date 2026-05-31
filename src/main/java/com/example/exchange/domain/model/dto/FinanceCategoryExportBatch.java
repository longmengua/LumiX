/*
 * 檔案用途：財務分類報表批次匯出 DTO，供每日 exporter job 與營運 API 使用。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record FinanceCategoryExportBatch(
        LocalDate reportDate,
        String exportBatchId,
        boolean balanced,
        Instant generatedAt,
        List<FinanceDailyReport> reports,
        List<String> blockers
) {
    public FinanceCategoryExportBatch {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        reports = reports == null ? List.of() : List.copyOf(reports);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
