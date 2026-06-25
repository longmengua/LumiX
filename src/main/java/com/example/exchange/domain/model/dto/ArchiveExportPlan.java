/*
 * 檔案用途：archive exporter 單一資料族的 export plan DTO。
 */
package com.example.exchange.domain.model.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class ArchiveExportPlan {

    private final String dataFamily;

    private final String source;

    private final String partition;

    private final String manifestRef;

    private final boolean deleteEligible;

    private final List<String> preconditions;
    public ArchiveExportPlan(String dataFamily, String source, String partition, String manifestRef, boolean deleteEligible, List<String> preconditions) {
        this.dataFamily = dataFamily;
        this.source = source;
        this.partition = partition;
        this.manifestRef = manifestRef;
        this.deleteEligible = deleteEligible;
        this.preconditions = preconditions;
    }

    public String dataFamily() {
        return dataFamily;
    }

    public String source() {
        return source;
    }

    public String partition() {
        return partition;
    }

    public String manifestRef() {
        return manifestRef;
    }

    public boolean deleteEligible() {
        return deleteEligible;
    }

    public List<String> preconditions() {
        return preconditions;
    }
}