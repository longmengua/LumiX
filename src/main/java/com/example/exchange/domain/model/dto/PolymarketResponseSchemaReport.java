/*
 * 檔案用途：領域 DTO，記錄 Polymarket 外部 API response schema compatibility 檢查結果。
 */
package com.example.exchange.domain.model.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * External response schema report.
 *
 * The report is intentionally lightweight: it keeps remote-field drift visible
 * without changing the existing Gamma/CLOB DTO contracts or persistence schema.
 */
@Data
@Builder
@Jacksonized
public class PolymarketResponseSchemaReport {

    private final String schemaVersion;

    private final String source;

    private final String endpoint;

    private final int itemCount;

    private final Set<String> missingFields;

    private final Set<String> unknownFields;

    private final boolean compatible;
    public PolymarketResponseSchemaReport(String schemaVersion, String source, String endpoint, int itemCount, Set<String> missingFields, Set<String> unknownFields, boolean compatible) {
        this.schemaVersion = schemaVersion;
        this.source = source;
        this.endpoint = endpoint;
        this.itemCount = itemCount;
        this.missingFields = missingFields;
        this.unknownFields = unknownFields;
        this.compatible = compatible;
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public String source() {
        return source;
    }

    public String endpoint() {
        return endpoint;
    }

    public int itemCount() {
        return itemCount;
    }

    public Set<String> missingFields() {
        return missingFields;
    }

    public Set<String> unknownFields() {
        return unknownFields;
    }

    public boolean compatible() {
        return compatible;
    }
}