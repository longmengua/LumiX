/*
 * 檔案用途：做市商 hedge decision 與 venue fill 對帳差異項目。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class HedgeReconciliationIssue {

    private final String marketMakerId;

    private final String symbol;

    private final String refId;

    private final String venueOrderId;

    private final String internalTradeRefId;

    private final String ledgerRefId;

    private final String reason;

    private final BigDecimal expectedNotional;

    private final BigDecimal filledNotional;
    public HedgeReconciliationIssue(String marketMakerId, String symbol, String refId, String venueOrderId, String internalTradeRefId, String ledgerRefId, String reason, BigDecimal expectedNotional, BigDecimal filledNotional) {
        expectedNotional = expectedNotional == null ? BigDecimal.ZERO : expectedNotional;
        filledNotional = filledNotional == null ? BigDecimal.ZERO : filledNotional;
    
        this.marketMakerId = marketMakerId;
        this.symbol = symbol;
        this.refId = refId;
        this.venueOrderId = venueOrderId;
        this.internalTradeRefId = internalTradeRefId;
        this.ledgerRefId = ledgerRefId;
        this.reason = reason;
        this.expectedNotional = expectedNotional;
        this.filledNotional = filledNotional;
    }

    public String marketMakerId() {
        return marketMakerId;
    }

    public String symbol() {
        return symbol;
    }

    public String refId() {
        return refId;
    }

    public String venueOrderId() {
        return venueOrderId;
    }

    public String internalTradeRefId() {
        return internalTradeRefId;
    }

    public String ledgerRefId() {
        return ledgerRefId;
    }

    public String reason() {
        return reason;
    }

    public BigDecimal expectedNotional() {
        return expectedNotional;
    }

    public BigDecimal filledNotional() {
        return filledNotional;
    }
}