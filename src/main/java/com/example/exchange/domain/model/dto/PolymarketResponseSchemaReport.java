/*
 * 檔案用途：領域 DTO，記錄 Polymarket 外部 API response schema compatibility 檢查結果。
 */
package com.example.exchange.domain.model.dto;

import java.util.Set;

/**
 * External response schema report.
 *
 * The report is intentionally lightweight: it keeps remote-field drift visible
 * without changing the existing Gamma/CLOB DTO contracts or persistence schema.
 */
public record PolymarketResponseSchemaReport(
        String schemaVersion,
        String source,
        String endpoint,
        int itemCount,
        Set<String> missingFields,
        Set<String> unknownFields,
        boolean compatible
) {
}
