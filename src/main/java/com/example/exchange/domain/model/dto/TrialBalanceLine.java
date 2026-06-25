/*
 * 檔案用途：trial balance 單一 account code 的借貸合計。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * Trial balance line by asset/account code.
 */
@Data
@Builder
@Jacksonized
public class TrialBalanceLine {

    private final String asset;

    private final String accountCode;

    private final BigDecimal debit;

    private final BigDecimal credit;

    private final BigDecimal netDebit;

    private final BigDecimal netCredit;
    public TrialBalanceLine(String asset, String accountCode, BigDecimal debit, BigDecimal credit, BigDecimal netDebit, BigDecimal netCredit) {
        this.asset = asset;
        this.accountCode = accountCode;
        this.debit = debit;
        this.credit = credit;
        this.netDebit = netDebit;
        this.netCredit = netCredit;
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