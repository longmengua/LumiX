/*
 * 檔案用途：做市商 quote reconciliation repair 的執行結果。
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
public class MarketMakerQuoteRepairReport {

    private final int checkedStates;

    private final int issueCount;

    private final int canceledOrders;

    private final int deactivatedStates;

    private final Instant generatedAt;

    private final List<MarketMakerQuoteRepairAction> actions;
    public MarketMakerQuoteRepairReport(int checkedStates, int issueCount, int canceledOrders, int deactivatedStates, Instant generatedAt, List<MarketMakerQuoteRepairAction> actions) {
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        actions = actions == null ? List.of() : List.copyOf(actions);
    
        this.checkedStates = checkedStates;
        this.issueCount = issueCount;
        this.canceledOrders = canceledOrders;
        this.deactivatedStates = deactivatedStates;
        this.generatedAt = generatedAt;
        this.actions = actions;
    }

    public int checkedStates() {
        return checkedStates;
    }

    public int issueCount() {
        return issueCount;
    }

    public int canceledOrders() {
        return canceledOrders;
    }

    public int deactivatedStates() {
        return deactivatedStates;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<MarketMakerQuoteRepairAction> actions() {
        return actions;
    }
}