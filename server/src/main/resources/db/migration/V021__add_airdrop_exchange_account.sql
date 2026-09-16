-- 受治理空投的交易所對應帳戶。每筆空投必須是可平衡的雙分錄，不能直接憑空增加使用者 projection。
-- 此 system principal 不可登入，僅為 accounts.user_id 的外鍵與 immutable ledger 對應方。
-- rollback：先停止空投並確認此帳戶沒有 ledger_entries、account_assets、outbox 或 audit 引用後，才可在維護窗口依序移除帳戶與 system user；不得刪除既有帳本資料。

INSERT INTO users (user_id, email, display_name, status)
VALUES ('system:airdrop', 'system-airdrop@lumix.internal', '系統空投帳戶', 'SUSPENDED')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO accounts (account_id, user_id, account_type, status, account_category)
VALUES ('system:airdrop:spot', 'system:airdrop', 'SPOT', 'ACTIVE', 'EXCHANGE')
ON CONFLICT (account_id) DO NOTHING;

COMMENT ON COLUMN accounts.account_category IS
'帳戶資金責任分類。USER 可 materialize 到使用者餘額；EXCHANGE 僅作雙分錄對應，不得當成使用者資產顯示。';
