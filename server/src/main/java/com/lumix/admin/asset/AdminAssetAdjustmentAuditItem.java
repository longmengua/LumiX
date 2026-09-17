package com.lumix.admin.asset;

import com.lumix.account.AccountType;
import java.time.Instant;
import java.util.Objects;

/**
 * 資產調整的唯讀對賬結果。
 *
 * <p>這個型別只描述既有 immutable journal、分錄與管理操作證據的交叉檢查結果；
 * 發現例外時只能交由人工調查，絕不能從查詢路徑觸發補帳或修改餘額。</p>
 */
public record AdminAssetAdjustmentAuditItem(
        long auditLogId,
        long ledgerJournalId,
        String actorId,
        String targetUserId,
        String targetUserEmail,
        AccountType accountType,
        String assetSymbol,
        String direction,
        String amount,
        String activityType,
        String note,
        String reconciliationStatus,
        Instant postedAt
) {
    public AdminAssetAdjustmentAuditItem {
        if (auditLogId <= 0 || ledgerJournalId <= 0) {
            throw new IllegalArgumentException("audit and journal identifiers must be positive");
        }
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(targetUserId, "targetUserId");
        Objects.requireNonNull(targetUserEmail, "targetUserEmail");
        Objects.requireNonNull(accountType, "accountType");
        Objects.requireNonNull(postedAt, "postedAt");
        // 例外 journal 可能缺少目標帳戶分錄；保留 null 才能讓營運人員看見缺口，而不是被 query 靜默排除。
        if ("VERIFIED".equals(reconciliationStatus)
                && (assetSymbol == null || amount == null || !("CREDIT".equals(direction) || "DEBIT".equals(direction)))) {
            throw new IllegalArgumentException("ledger direction must be canonical");
        }
        if (!("VERIFIED".equals(reconciliationStatus) || "EXCEPTION".equals(reconciliationStatus))) {
            throw new IllegalArgumentException("reconciliation status must be canonical");
        }
    }
}
