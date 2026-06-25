/*
 * 檔案用途：財務日報單列 DTO，依 reason / asset / account code 彙總 ledger postings。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


@Data
@Builder
@Jacksonized
public class FinanceDailyReportLine {

    private final String reason;

    private final String asset;

    private final String accountCode;

    private final BigDecimal debit;

    private final BigDecimal credit;

    private final BigDecimal netDebit;

    private final BigDecimal netCredit;
    public FinanceDailyReportLine(String reason, String asset, String accountCode, BigDecimal debit, BigDecimal credit, BigDecimal netDebit, BigDecimal netCredit) {
        debit = debit == null ? BigDecimal.ZERO : debit;
        credit = credit == null ? BigDecimal.ZERO : credit;
        BigDecimal net = debit.subtract(credit);
        netDebit = net.signum() > 0 ? net : BigDecimal.ZERO;
        netCredit = net.signum() < 0 ? net.abs() : BigDecimal.ZERO;
    
        this.reason = reason;
        this.asset = asset;
        this.accountCode = accountCode;
        this.debit = debit;
        this.credit = credit;
        this.netDebit = netDebit;
        this.netCredit = netCredit;
    }

    public String reason() {
        return reason;
    }

    public String asset() {
        return asset;
    }

    public String accountCode() {
        return accountCode;
    }

    public BigDecimal debit() {
        return debit;
    }

    public BigDecimal credit() {
        return credit;
    }

    public BigDecimal netDebit() {
        return netDebit;
    }

    public BigDecimal netCredit() {
        return netCredit;
    }
}