package com.lumix.account.history;

import java.time.Instant;
import java.util.Objects;

/**
 * 資產歷史的 keyset cursor。
 *
 * <p>同一 postedAt 可能有多筆 ledger entry，故必須與 entryId 成對使用，避免以 offset 翻頁時因新增資料
 * 而重複或遺漏。此值只代表讀取位置，不是可指定 owner 或帳戶的授權憑證。</p>
 */
public record AssetLedgerHistoryCursor(Instant postedAt, long entryId) {
    public AssetLedgerHistoryCursor {
        Objects.requireNonNull(postedAt, "postedAt must not be null");
        if (entryId <= 0L) {
            throw new IllegalArgumentException("entryId must be positive");
        }
    }
}
