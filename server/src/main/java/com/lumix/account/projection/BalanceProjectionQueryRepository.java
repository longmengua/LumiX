package com.lumix.account.projection;

import java.util.List;

/**
 * balance projection 的 owner-scoped 唯讀查詢邊界。
 *
 * <p>介面只接收已由 server session 確認的 userId，沒有任意 accountId 或其他使用者 userId 查詢入口；
 * 這可讓資料庫 adapter 固定將 owner predicate 放在查詢中。</p>
 */
interface BalanceProjectionQueryRepository {

    List<AccountBalanceProjection> findByOwnerUserId(String userId);
}
