-- 為既有使用者補齊資產帳戶容器；本 migration 不新增任何餘額、ledger entry 或 balance projection。
-- account_id 由 immutable user_id 與固定帳戶類型組成，與新註冊 transaction 的 provisioning 規則一致。

INSERT INTO accounts (account_id, user_id, account_type, status)
SELECT u.user_id || ':spot', u.user_id, 'SPOT', 'ACTIVE'
FROM users u
ON CONFLICT (user_id, account_type) DO NOTHING;

INSERT INTO accounts (account_id, user_id, account_type, status)
SELECT u.user_id || ':futures', u.user_id, 'FUTURES', 'ACTIVE'
FROM users u
ON CONFLICT (user_id, account_type) DO NOTHING;

INSERT INTO accounts (account_id, user_id, account_type, status)
SELECT u.user_id || ':margin', u.user_id, 'MARGIN', 'ACTIVE'
FROM users u
ON CONFLICT (user_id, account_type) DO NOTHING;
