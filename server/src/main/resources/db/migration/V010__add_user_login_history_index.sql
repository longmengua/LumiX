-- P29-R04: 使用者登入紀錄只讀 API 的查詢索引。
-- user_sessions 已是成功登入 session 的權威來源；本 migration 不新增認證材料、不記錄 IP/user agent，
-- 也不修改或刪除既有 session，以避免把安全歷程與現有 session 驗證語意混在一起。

CREATE INDEX idx_user_sessions_login_history
    ON user_sessions (user_id, created_at DESC);

COMMENT ON INDEX idx_user_sessions_login_history IS
'依使用者倒序讀取成功登入 session 的 bounded login-history API 索引；不包含 token digest 或其他秘密欄位。';
