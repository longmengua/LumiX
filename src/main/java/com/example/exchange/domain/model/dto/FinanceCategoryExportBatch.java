/*
 * 檔案用途：財務分類報表批次匯出 DTO，供每日 exporter job 與營運 API 使用。
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
public class FinanceCategoryExportBatch {

    private final LocalDate reportDate;

    private final String exportBatchId;

    private final boolean balanced;

    private final Instant generatedAt;

    private final List<FinanceDailyReport> reports;

    private final List<String> blockers;
    public FinanceCategoryExportBatch(LocalDate reportDate, String exportBatchId, boolean balanced, Instant generatedAt, List<FinanceDailyReport> reports, List<String> blockers) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        reports = reports == null ? List.of() : List.copyOf(reports);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    
        this.reportDate = reportDate;
        this.exportBatchId = exportBatchId;
        this.balanced = balanced;
        this.generatedAt = generatedAt;
        this.reports = reports;
        this.blockers = blockers;
    }

    public LocalDate reportDate() {
        return reportDate;
    }

    public String exportBatchId() {
        return exportBatchId;
    }

    public boolean balanced() {
        return balanced;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<FinanceDailyReport> reports() {
        return reports;
    }

    public List<String> blockers() {
        return blockers;
    }
}