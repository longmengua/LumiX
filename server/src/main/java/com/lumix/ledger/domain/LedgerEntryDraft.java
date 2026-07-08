package com.lumix.ledger.domain;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 單筆 ledger entry draft。
 *
 * 這份 draft 只描述 posting 前的 domain 資料，不負責寫入 ledger，也不負責更新任何 balance。
 */
public record LedgerEntryDraft(
        AccountId accountId,
        AssetSymbol assetSymbol,
        LedgerDirection direction,
        BigDecimal amount,
        long entrySequence
) {

    public LedgerEntryDraft {
        // ledger entry draft 只接受可審計、可重建的最小資訊，避免把 runtime 資料混進 domain contract。
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(assetSymbol, "assetSymbol must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (entrySequence <= 0L) {
            throw new IllegalArgumentException("entrySequence must be greater than zero");
        }
    }
}
