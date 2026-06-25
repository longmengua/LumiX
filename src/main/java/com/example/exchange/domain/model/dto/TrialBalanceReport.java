/*
 * 檔案用途：trial balance 結果，用於財務對帳與報表基礎。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * Trial balance report for a scoped ledger query.
 */
@Data
@Builder
@Jacksonized
public class TrialBalanceReport {

    private final long uid;

    private final String asset;

    private final BigDecimal totalDebit;

    private final BigDecimal totalCredit;

    private final boolean balanced;

    private final List<TrialBalanceLine> lines;
    public TrialBalanceReport(long uid, String asset, BigDecimal totalDebit, BigDecimal totalCredit, boolean balanced, List<TrialBalanceLine> lines) {
        this.uid = uid;
        this.asset = asset;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.balanced = balanced;
        this.lines = lines;
    }

    public long uid() {
        return uid;
    }

    public String asset() {
        return asset;
    }

    public BigDecimal totalDebit() {
        return totalDebit;
    }

    public BigDecimal totalCredit() {
        return totalCredit;
    }

    public boolean balanced() {
        return balanced;
    }

    public List<TrialBalanceLine> lines() {
        return lines;
    }
}