/*
 * 檔案用途：archive exporter skeleton 單次執行報告。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ArchiveExporterRunReport(
        boolean enabled,
        LocalDate archiveDate,
        Instant generatedAt,
        List<ArchiveExportPlan> plans
) {
}
