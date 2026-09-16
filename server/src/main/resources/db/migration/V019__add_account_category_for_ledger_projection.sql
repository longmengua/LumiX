-- 資產 runtime：區分使用者資產帳戶與交易所對應帳戶。
-- 目的：double-entry 的對應 debit / credit 必須可落在 exchange account，但使用者餘額 projection
-- 只能 materialize USER account，否則真實入帳的對應分錄會被誤判為負的使用者餘額。
-- rollback：確認沒有 runtime 依賴 account_category，且 EXCHANGE 帳戶均已結清並停止服務後，才可移除
-- 本欄位與 constraint；不得藉 rollback 刪除 ledger 或 balance projection 資料。

ALTER TABLE accounts
    ADD COLUMN account_category VARCHAR(16) NOT NULL DEFAULT 'USER';

ALTER TABLE accounts
    ADD CONSTRAINT ck_accounts_category
        CHECK (account_category IN ('USER', 'EXCHANGE'));

CREATE INDEX idx_accounts_category ON accounts (account_category);

COMMENT ON COLUMN accounts.account_category IS
'帳戶資金責任分類。USER 可 materialize 到使用者餘額；EXCHANGE 僅作雙分錄對應，不得當成使用者資產顯示。';
