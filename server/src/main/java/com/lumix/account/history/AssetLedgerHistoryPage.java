package com.lumix.account.history;

import java.util.List;
import java.util.Objects;

/** 唯讀歷史的一頁資料；nextCursor 為 null 即代表沒有更舊的既有 entry。 */
public record AssetLedgerHistoryPage(List<AssetLedgerHistoryItem> items, AssetLedgerHistoryCursor nextCursor) {
    public AssetLedgerHistoryPage {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }
}
