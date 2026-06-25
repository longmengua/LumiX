/*
 * 檔案用途：做市商 hedge strategy execution 的彙總結果。
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
public class HedgeExecutionReport {

    private final String marketMakerId;

    private final int exposureCount;

    private final int plannedCount;

    private final int routedCount;

    private final Instant generatedAt;

    private final List<HedgeStrategyDecision> strategyDecisions;

    private final List<HedgeDecision> hedgeDecisions;
    public HedgeExecutionReport(String marketMakerId, int exposureCount, int plannedCount, int routedCount, Instant generatedAt, List<HedgeStrategyDecision> strategyDecisions, List<HedgeDecision> hedgeDecisions) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        strategyDecisions = strategyDecisions == null ? List.of() : List.copyOf(strategyDecisions);
        hedgeDecisions = hedgeDecisions == null ? List.of() : List.copyOf(hedgeDecisions);
    
        this.marketMakerId = marketMakerId;
        this.exposureCount = exposureCount;
        this.plannedCount = plannedCount;
        this.routedCount = routedCount;
        this.generatedAt = generatedAt;
        this.strategyDecisions = strategyDecisions;
        this.hedgeDecisions = hedgeDecisions;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public int exposureCount() {
        return exposureCount;
    }

    public int plannedCount() {
        return plannedCount;
    }

    public int routedCount() {
        return routedCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<HedgeStrategyDecision> strategyDecisions() {
        return strategyDecisions;
    }

    public List<HedgeDecision> hedgeDecisions() {
        return hedgeDecisions;
    }
}