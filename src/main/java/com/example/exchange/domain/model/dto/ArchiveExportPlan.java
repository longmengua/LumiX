/*
 * 檔案用途：archive exporter 單一資料族的 export plan DTO。
 */
package com.example.exchange.domain.model.dto;

import java.util.List;

public record ArchiveExportPlan(
        String dataFamily,
        String source,
        String partition,
        String manifestRef,
        boolean deleteEligible,
        List<String> preconditions
) {
}
