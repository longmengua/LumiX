package com.lumix.admin.asset;

import java.util.List;

/** 管理端帳戶容器的唯讀資料邊界；不得在此介面加入開戶或帳戶狀態變更能力。 */
interface AdminUserAccountInventoryQueryRepository {
    List<AdminUserAccountInventory> findByUserId(String userId);
}
