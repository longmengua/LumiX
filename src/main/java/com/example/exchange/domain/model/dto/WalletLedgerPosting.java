/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;


/**
 * 雙式簿記的一條分錄。
 */
@Data
@Builder
@Jacksonized
public class WalletLedgerPosting {

    private final String accountCode;

    private final String asset;

    private final BigDecimal debit;

    private final BigDecimal credit;
    public WalletLedgerPosting(String accountCode, String asset, BigDecimal debit, BigDecimal credit) {
        debit = debit == null ? BigDecimal.ZERO : debit;
        credit = credit == null ? BigDecimal.ZERO : credit;
    
        this.accountCode = accountCode;
        this.asset = asset;
        this.debit = debit;
        this.credit = credit;
    }

    public String accountCode() {
        return accountCode;
    }

    public String asset() {
        return asset;
    }

    public BigDecimal debit() {
        return debit;
    }

    public BigDecimal credit() {
        return credit;
    }
}