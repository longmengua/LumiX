package com.lumix.admin.user;

import java.util.List;

/**
 * 唯讀使用者搜尋的 bounded page。
 *
 * <p>total 不套用 cursor，代表同一組篩選條件的完整結果數；nextCursor 仍只控制目前 keyset page，兩者不可混用。</p>
 */
record AdminUserSearchPage(List<AdminUserSummary> items, AdminUserSearchCursor nextCursor, long total, int pageSize) { }
