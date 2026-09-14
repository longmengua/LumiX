package com.lumix.admin.user;

import java.time.Instant;

/**
 * 使用者清單的 keyset 游標。
 *
 * <p>建立時間本身可能相同，因此必須連同 userId 作為穩定排序鍵；不可改回 offset pagination，否則資料量增加後
 * 會讓後續頁面必須跳過大量資料。</p>
 */
record AdminUserSearchCursor(Instant createdAt, String userId) { }
