/*
 * 檔案用途：領域模型或持久化實體，承載交易、帳戶、持倉與預測市場狀態。
 */
package com.example.exchange.domain.model.entity;

import java.math.BigDecimal;

/**
 * 雙式簿記的一條分錄。
 */
public record WalletLedgerPosting(
        String accountCode,
        String asset,
        BigDecimal debit,
        BigDecimal credit
) {
    public WalletLedgerPosting {
        debit = debit == null ? BigDecimal.ZERO : debit;
        credit = credit == null ? BigDecimal.ZERO : credit;
    }
}
