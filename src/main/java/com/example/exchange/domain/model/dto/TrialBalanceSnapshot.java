/*
 * 檔案用途：trial balance 每日快照 DTO，保存財務日結可重查的固定結果。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class TrialBalanceSnapshot {

    private final LocalDate reportDate;

    private final long uid;

    private final String asset;

    private final BigDecimal totalDebit;

    private final BigDecimal totalCredit;

    private final boolean balanced;

    private final Instant generatedAt;

    private final List<TrialBalanceLine> lines;
    public TrialBalanceSnapshot(LocalDate reportDate, long uid, String asset, BigDecimal totalDebit, BigDecimal totalCredit, boolean balanced, Instant generatedAt, List<TrialBalanceLine> lines) {
        totalDebit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        totalCredit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        lines = lines == null ? List.of() : List.copyOf(lines);
    
        this.reportDate = reportDate;
        this.uid = uid;
        this.asset = asset;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.balanced = balanced;
        this.generatedAt = generatedAt;
        this.lines = lines;
    }

    public static TrialBalanceSnapshot from(LocalDate reportDate, TrialBalanceReport report) {
        return new TrialBalanceSnapshot(
                reportDate,
                report.uid(),
                report.asset(),
                report.totalDebit(),
                report.totalCredit(),
                report.balanced(),
                Instant.now(),
                report.lines()
        );
    }

    public LocalDate reportDate() {
        return reportDate;
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

    public Instant generatedAt() {
        return generatedAt;
    }

    public List<TrialBalanceLine> lines() {
        return lines;
    }
}