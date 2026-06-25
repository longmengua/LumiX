/*
 * 檔案用途：ADL queue、liquidated position coverage 與 insurance fund balance 的 reconciliation report。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class AdlInsuranceReconciliationReport {

    private final String asset;

    private final BigDecimal insuranceFundBalance;

    private final int openAdlQueueCount;

    private final BigDecimal openAdlQueueAmount;

    private final int issueCount;

    private final Instant generatedAt;

    private final List<AdlInsuranceReconciliationIssue> issues;
    public AdlInsuranceReconciliationReport(String asset, BigDecimal insuranceFundBalance, int openAdlQueueCount, BigDecimal openAdlQueueAmount, int issueCount, Instant generatedAt, List<AdlInsuranceReconciliationIssue> issues) {
        insuranceFundBalance = insuranceFundBalance == null ? BigDecimal.ZERO : insuranceFundBalance;
        openAdlQueueAmount = openAdlQueueAmount == null ? BigDecimal.ZERO : openAdlQueueAmount;
        issues = issues == null ? List.of() : List.copyOf(issues);
    
        this.asset = asset;
        this.insuranceFundBalance = insuranceFundBalance;
        this.openAdlQueueCount = openAdlQueueCount;
        this.openAdlQueueAmount = openAdlQueueAmount;
        this.issueCount = issueCount;
        this.generatedAt = generatedAt;
        this.issues = issues;
    }

    public String asset() {
        return asset;
    }

    public BigDecimal insuranceFundBalance() {
        return insuranceFundBalance;
    }

    public int openAdlQueueCount() {
        return openAdlQueueCount;
    }

    public BigDecimal openAdlQueueAmount() {
        return openAdlQueueAmount;
    }

    public int issueCount() {
        return issueCount;
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<AdlInsuranceReconciliationIssue> issues() {
        return issues;
    }
}