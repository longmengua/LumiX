/*
 * 檔案用途：ADL queue 營運告警報表 DTO。
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
public class AdlOperationalAlertReport {

    private final int alertCount;

    private final Instant generatedAt;

    private final List<AdlOperationalAlert> alerts;
    public AdlOperationalAlertReport(int alertCount, Instant generatedAt, List<AdlOperationalAlert> alerts) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    
        this.alertCount = alertCount;
        this.generatedAt = generatedAt;
        this.alerts = alerts;
    }

    public int alertCount() {
        return alertCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<AdlOperationalAlert> alerts() {
        return alerts;
    }
}