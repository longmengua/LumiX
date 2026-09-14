-- P27 使用者唯讀檢視的索引。
-- 此 migration 僅改善最高管理員的 bounded read query；不建立帳戶狀態、角色、資產或帳本的任何寫入能力。
-- 名稱索引專供 lower(display_name) 的前綴 LIKE 使用，查詢端不得改成 '%keyword%'，否則索引無法縮小候選資料。

CREATE INDEX idx_users_admin_created_cursor
    ON users (created_at DESC, user_id DESC);

CREATE INDEX idx_users_admin_display_name_prefix
    ON users (lower(display_name) text_pattern_ops);

COMMENT ON INDEX idx_users_admin_created_cursor IS
'管理端使用者清單依註冊時間與 user_id 倒序翻頁的 keyset 索引；不包含任何認證秘密。';

COMMENT ON INDEX idx_users_admin_display_name_prefix IS
'管理端不分大小寫顯示名稱前綴搜尋索引，僅支援 keyword% 而非 %keyword%。';
