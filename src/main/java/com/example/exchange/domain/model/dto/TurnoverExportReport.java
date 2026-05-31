/*
 * 檔案用途：Turnover export read model，保存查詢條件、彙總與明細列。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record TurnoverExportReport(
        String filename,
        Instant generatedAt,
        TurnoverSummary summary,
        List<String> headers,
        List<List<String>> rows
) {
    public TurnoverExportReport {
        headers = headers == null ? List.of() : List.copyOf(headers);
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
    }
}
