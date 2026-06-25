/*
 * 檔案用途：體驗金 campaign export read model，提供營運可直接下載/轉檔的列資料。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class BonusCreditCampaignExport {

    private final String filename;

    private final Instant generatedAt;

    private final BonusCreditCampaignReport summary;

    private final List<String> headers;

    private final List<List<String>> rows;
    public BonusCreditCampaignExport(String filename, Instant generatedAt, BonusCreditCampaignReport summary, List<String> headers, List<List<String>> rows) {
        headers = headers == null ? List.of() : List.copyOf(headers);
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
    
        this.filename = filename;
        this.generatedAt = generatedAt;
        this.summary = summary;
        this.headers = headers;
        this.rows = rows;
    }

    public String filename() {
        return filename;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public BonusCreditCampaignReport summary() {
        return summary;
    }

    public List<String> headers() {
        return headers;
    }

    public List<List<String>> rows() {
        return rows;
    }
}