package com.lumix.admin.asset;

import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.time.Instant;
import java.util.Objects;

/** 管理支援所需的指定使用者 immutable ledger line；不含任何對手方帳戶資訊。 */
public record AdminUserLedgerHistoryItem(long entryId, AccountType accountType, AssetSymbol assetSymbol, String direction,
                                         MoneyAmount amount, String referenceType, String referenceId, Instant postedAt) {
    public AdminUserLedgerHistoryItem {
        if (entryId <= 0) throw new IllegalArgumentException("entryId must be positive");
        Objects.requireNonNull(accountType, "accountType"); Objects.requireNonNull(assetSymbol, "assetSymbol");
        Objects.requireNonNull(amount, "amount"); Objects.requireNonNull(postedAt, "postedAt");
        if (!amount.isPositive() || !("CREDIT".equals(direction) || "DEBIT".equals(direction))) throw new IllegalArgumentException("ledger line must be canonical");
    }
}
