-- P29-R12: 同平台裝置替換後的資金外流保護狀態。
-- 此欄位只保存安全截止時間，不執行提幣、帳戶間轉帳、帳本或餘額 mutation。

ALTER TABLE users
    ADD COLUMN fund_transfer_restricted_until TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN users.fund_transfer_restricted_until IS
'同平台裝置完成替換後，提款與帳戶間轉帳必須在此時間前 fail closed 的安全截止時間；不可由前端倒數或使用者輸入解除。';
