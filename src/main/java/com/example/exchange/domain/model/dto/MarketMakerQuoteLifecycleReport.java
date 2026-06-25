/*
 * 檔案用途：做市商 quote lifecycle 執行結果，連結 quote decision 與實際內部訂單。
 */
package com.example.exchange.domain.model.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class MarketMakerQuoteLifecycleReport {

    private final MarketMakerQuoteDecision decision;

    private final int canceledCount;

    private final int placedCount;

    private final UUID bidOrderId;

    private final UUID askOrderId;
    public MarketMakerQuoteLifecycleReport(MarketMakerQuoteDecision decision, int canceledCount, int placedCount, UUID bidOrderId, UUID askOrderId) {
        this.decision = decision;
        this.canceledCount = canceledCount;
        this.placedCount = placedCount;
        this.bidOrderId = bidOrderId;
        this.askOrderId = askOrderId;
    }

    public MarketMakerQuoteDecision decision() {
        return decision;
    }

    public int canceledCount() {
        return canceledCount;
    }

    public int placedCount() {
        return placedCount;
    }

    public UUID bidOrderId() {
        return bidOrderId;
    }

    public UUID askOrderId() {
        return askOrderId;
    }
}