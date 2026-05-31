/*
 * 檔案用途：體驗金 campaign export read model，提供營運可直接下載/轉檔的列資料。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record BonusCreditCampaignExport(
        String filename,
        Instant generatedAt,
        BonusCreditCampaignReport summary,
        List<String> headers,
        List<List<String>> rows
) {
    public BonusCreditCampaignExport {
        headers = headers == null ? List.of() : List.copyOf(headers);
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
    }
}
