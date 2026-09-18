package com.lumix.admin.asset;

import java.util.Locale;

/**
 * 管理端資產調整的受治理類型。
 *
 * <p>HTTP contract 仍以既有 {@code activityId} 欄位傳遞，以免破壞既有 API；這個 enum 只在
 * server 邊界把可執行類型收斂為明確集合，未知類型必須 fail closed。</p>
 */
enum AdminAssetAdjustmentType {
    AIRDROP;

    static AdminAssetAdjustmentType fromActivityId(String activityId) {
        try {
            return valueOf(activityId.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported asset adjustment activityId", exception);
        }
    }
}
