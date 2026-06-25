/*
 * 檔案用途：ADL/insurance-fund shortfall reconciliation 的單筆差異。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class AdlInsuranceReconciliationIssue {

    private final String liquidationId;

    private final long uid;

    private final String symbol;

    private final String reason;

    private final BigDecimal queueAmount;

    private final BigDecimal positionAdlCovered;

    private final BigDecimal positionInsuranceCovered;

    private final String queueOwner;
    public AdlInsuranceReconciliationIssue(String liquidationId, long uid, String symbol, String reason, BigDecimal queueAmount, BigDecimal positionAdlCovered, BigDecimal positionInsuranceCovered, String queueOwner) {
        queueAmount = queueAmount == null ? BigDecimal.ZERO : queueAmount;
        positionAdlCovered = positionAdlCovered == null ? BigDecimal.ZERO : positionAdlCovered;
        positionInsuranceCovered = positionInsuranceCovered == null ? BigDecimal.ZERO : positionInsuranceCovered;
    
        this.liquidationId = liquidationId;
        this.uid = uid;
        this.symbol = symbol;
        this.reason = reason;
        this.queueAmount = queueAmount;
        this.positionAdlCovered = positionAdlCovered;
        this.positionInsuranceCovered = positionInsuranceCovered;
        this.queueOwner = queueOwner;
    }

    public String liquidationId() {
        return liquidationId;
    }

    public long uid() {
        return uid;
    }

    public String symbol() {
        return symbol;
    }

    public String reason() {
        return reason;
    }

    public BigDecimal queueAmount() {
        return queueAmount;
    }

    public BigDecimal positionAdlCovered() {
        return positionAdlCovered;
    }

    public BigDecimal positionInsuranceCovered() {
        return positionInsuranceCovered;
    }

    public String queueOwner() {
        return queueOwner;
    }
}