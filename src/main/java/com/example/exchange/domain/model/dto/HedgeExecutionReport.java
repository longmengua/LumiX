/*
 * 檔案用途：做市商 hedge strategy execution 的彙總結果。
 */
package com.example.exchange.domain.model.dto;

import java.time.Instant;
import java.util.List;

public record HedgeExecutionReport(
        String marketMakerId,
        int exposureCount,
        int plannedCount,
        int routedCount,
        Instant generatedAt,
        List<HedgeStrategyDecision> strategyDecisions,
        List<HedgeDecision> hedgeDecisions
) {
    public HedgeExecutionReport {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        strategyDecisions = strategyDecisions == null ? List.of() : List.copyOf(strategyDecisions);
        hedgeDecisions = hedgeDecisions == null ? List.of() : List.copyOf(hedgeDecisions);
    }
}
