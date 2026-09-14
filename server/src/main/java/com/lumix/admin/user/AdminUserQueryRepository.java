package com.lumix.admin.user;

import java.util.List;
import java.util.Optional;

/** 使用者管理只讀投影的持久化介面，禁止加入停權、角色或帳務 mutation。 */
interface AdminUserQueryRepository {

    /**
     * 以已驗證條件讀取 bounded 資料列。
     *
     * <p>呼叫端會要求比單頁多一筆以判斷是否還有下一頁；完整總數由獨立的 count 查詢提供，不能從 cursor page 推估。</p>
     */
    List<AdminUserSummary> find(AdminUserSearchCriteria criteria, int limit);

    /**
     * 計算目前篩選條件的完整結果數，故刻意忽略 keyset cursor。
     *
     * <p>這個數字只作為管理清單的頁碼與總數資訊，不能用來推導下一頁資料位置。</p>
     */
    long count(AdminUserSearchCriteria criteria);

    Optional<AdminUserDetail> findById(String userId);
}
