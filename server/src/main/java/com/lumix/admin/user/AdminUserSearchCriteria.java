package com.lumix.admin.user;

import java.time.Instant;

/**
 * 管理端唯讀使用者清單的已驗證查詢條件。
 *
 * <p>名稱條件代表不分大小寫的前綴搜尋；萬用字元只由 repository 附在尾端，避免前綴萬用字元破壞索引使用。</p>
 */
record AdminUserSearchCriteria(
    String displayNamePrefix,
    Instant createdFrom,
    Instant createdBefore,
    Instant lastLoginFrom,
    Instant lastLoginBefore,
    AdminUserSearchCursor cursor
) { }
