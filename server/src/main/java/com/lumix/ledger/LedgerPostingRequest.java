package com.lumix.ledger;

import com.lumix.account.AccountId;
import com.lumix.account.AssetSymbol;
import com.lumix.account.UserId;
import com.lumix.common.MoneyAmount;
import com.lumix.common.RequestId;

import java.util.Objects;

/**
 * 單筆帳本分錄請求。
 * Phase 9 只定義資料與最基本驗證，不做任何實際扣帳或入帳。
 */
public record LedgerPostingRequest(
        RequestId requestId,
        UserId userId,
        AccountId accountId,
        LedgerAccountType accountType,
        AssetSymbol asset,
        MoneyAmount amount,
        LedgerEntryDirection direction,
        String businessType,
        String reason,
        String referenceId
) {

    public LedgerPostingRequest {
        // 分錄必須明確指向一個 request，方便日後做冪等與稽核。
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(accountType, "accountType must not be null");
        Objects.requireNonNull(asset, "asset must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        businessType = requireText(businessType, "businessType");
        reason = requireText(reason, "reason");
        referenceId = requireText(referenceId, "referenceId");

        // 帳本分錄金額在 Phase 9 只接受正數，方向由 direction 表示。
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }

    private static String requireText(String value, String fieldName) {
        // 業務類型、原因與 referenceId 都是後續對帳與稽核的必要欄位。
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
