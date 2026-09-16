package com.lumix.account.history;

import com.lumix.account.AccountType;
import com.lumix.account.AssetSymbol;
import com.lumix.common.MoneyAmount;
import java.time.Instant;
import java.util.Objects;

/**
 * 登入者所屬帳戶的一筆 immutable ledger line 唯讀表示。
 *
 * <p>amount 維持正數，增減方向由 direction 表示；不能在讀取層自行相減或當成可用餘額。reference 是既有
 * journal business reference，僅供該 owner 的事件追溯，不能推導任何對手方帳戶資料。</p>
 */
public record AssetLedgerHistoryItem(
    long entryId,
    long journalId,
    AccountType accountType,
    AssetSymbol assetSymbol,
    String direction,
    MoneyAmount amount,
    String referenceType,
    String referenceId,
    Instant postedAt,
    Instant recordedAt
) {
    public AssetLedgerHistoryItem {
        if (entryId <= 0L || journalId <= 0L) {
            throw new IllegalArgumentException("ledger identifiers must be positive");
        }
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(assetSymbol, "assetSymbol must not be null");
        direction = requireOneOf(direction, "direction", "DEBIT", "CREDIT");
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
        referenceType = requireText(referenceType, "referenceType");
        referenceId = requireText(referenceId, "referenceId");
        Objects.requireNonNull(postedAt, "postedAt must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }

    private static String requireOneOf(String value, String name, String... allowed) {
        String normalized = requireText(value, name);
        for (String candidate : allowed) {
            if (candidate.equals(normalized)) return normalized;
        }
        throw new IllegalArgumentException(name + " has unsupported value");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return normalized;
    }
}
