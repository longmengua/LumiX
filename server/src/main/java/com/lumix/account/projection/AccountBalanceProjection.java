package com.lumix.account.projection;

import com.lumix.account.AccountId;
import com.lumix.account.AccountStatus;
import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.time.Instant;
import java.util.Objects;

/**
 * 對 authenticated owner 開放的帳戶資產 projection。
 *
 * <p>本模型只代表資料庫中的 query-side snapshot；資金真相仍應由 immutable ledger 與後續對帳流程判定。
 * 它不含 userId，避免 controller 或前端把 owner identity 當成可由 request 指定的查詢條件。</p>
 */
public record AccountBalanceProjection(
    AccountId accountId,
    AccountType accountType,
    AccountStatus accountStatus,
    AssetSymbol assetSymbol,
    String assetDisplayName,
    String assetStatus,
    int precisionScale,
    MoneyAmount total,
    MoneyAmount available,
    MoneyAmount locked,
    long projectionVersion,
    Instant projectedAt,
    Instant reconciledAt
) {

    public AccountBalanceProjection {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(accountStatus, "accountStatus must not be null");
        Objects.requireNonNull(assetSymbol, "assetSymbol must not be null");
        assetDisplayName = requireText(assetDisplayName, "assetDisplayName");
        assetStatus = requireText(assetStatus, "assetStatus");
        if (precisionScale < 0 || precisionScale > 18) {
            throw new IllegalArgumentException("precisionScale must be between 0 and 18");
        }
        Objects.requireNonNull(total, "total must not be null");
        Objects.requireNonNull(available, "available must not be null");
        Objects.requireNonNull(locked, "locked must not be null");
        if (total.isNegative() || available.isNegative() || locked.isNegative()) {
            throw new IllegalArgumentException("projected balances must not be negative");
        }
        if (total.value().compareTo(available.value().add(locked.value())) != 0) {
            throw new IllegalArgumentException("total must equal available plus locked");
        }
        if (projectionVersion < 0L) {
            throw new IllegalArgumentException("projectionVersion must not be negative");
        }
        Objects.requireNonNull(projectedAt, "projectedAt must not be null");
    }

    /** 回傳 presentation 層應顯示的對帳證據，而不是自行推測資金可用性。 */
    public BalanceProjectionFreshness freshness() {
        return BalanceProjectionFreshness.from(projectedAt, reconciledAt);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
