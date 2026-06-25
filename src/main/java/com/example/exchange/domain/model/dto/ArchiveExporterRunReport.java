/*
 * 檔案用途：archive exporter skeleton 單次執行報告。
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
public class ArchiveExporterRunReport {

    private final boolean enabled;

    private final LocalDate archiveDate;

    private final Instant generatedAt;

    private final List<ArchiveExportPlan> plans;
    public ArchiveExporterRunReport(boolean enabled, LocalDate archiveDate, Instant generatedAt, List<ArchiveExportPlan> plans) {
        this.enabled = enabled;
        this.archiveDate = archiveDate;
        this.generatedAt = generatedAt;
        this.plans = plans;
    }

    public boolean enabled() {
        return enabled;
    }

    public LocalDate archiveDate() {
        return archiveDate;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<ArchiveExportPlan> plans() {
        return plans;
    }
}