package com.lumix.account.history;

import java.util.List;
import java.util.Optional;

/** 資產歷史的資料庫唯讀邊界；實作者不得在此介面下新增 ledger 或更新餘額。 */
interface AssetLedgerHistoryQueryRepository {
    List<AssetLedgerHistoryItem> findByOwnerUserId(String userId, Optional<AssetLedgerHistoryCursor> before, int limit);
}
