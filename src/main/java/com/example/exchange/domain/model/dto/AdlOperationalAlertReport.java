/*
 * 檔案用途：ADL queue 營運告警報表 DTO。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record AdlOperationalAlertReport(
        int alertCount,
        Instant generatedAt,
        List<AdlOperationalAlert> alerts
) {
    public AdlOperationalAlertReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }
}
