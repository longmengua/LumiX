package com.lumix.admin.asset;

import java.util.List;

/** 管理端唯讀 asset projection 資料邊界；禁止擴充為 admin balance adjustment。 */
interface AdminUserAssetQueryRepository {
    List<AdminUserAssetProjection> findByUserId(String userId);
}
