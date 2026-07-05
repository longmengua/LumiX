package com.lumix.account;

import com.lumix.common.MoneyAmount;

import java.util.Objects;

/**
 * 單一資產帳戶的唯讀視圖。
 * 這是 Phase 9 的查詢模型，不能被拿來當作可變資產實體。
 */
public record AssetAccountView(
        UserId userId,
        AccountId accountId,
        AccountType accountType,
        AssetSymbol asset,
        MoneyAmount total,
        MoneyAmount available,
        MoneyAmount locked,
        AccountStatus status
) implements AccountBalanceView {

    public AssetAccountView {
        // 所有欄位都必須明確存在，避免帳戶查詢時留下半成品資料。
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(asset, "asset must not be null");
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(available, "available must not be null");
        Objects.requireNonNull(locked, "locked must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
