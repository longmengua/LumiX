package com.lumix.admin.asset;

import java.util.List;

/**
 * 資產調整對賬的唯讀資料邊界。
 *
 * <p>實作只能讀取已存在的 audit、journal 與 ledger entries；不得把查詢結果接成任何修正命令。</p>
 */
interface AdminAssetAdjustmentAuditQueryRepository {
    List<AdminAssetAdjustmentAuditItem> findLatest(int limit);
}
