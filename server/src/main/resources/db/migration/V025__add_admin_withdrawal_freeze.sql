-- 管理端提幣凍結狀態。此欄位只描述受治理的出金限制，不保存私鑰、鏈上資訊或餘額。
-- Rollback：確認所有凍結均已解除並完成稽核匯出後，才可執行 ALTER TABLE users DROP COLUMN withdrawal_frozen_at。
ALTER TABLE users
    ADD COLUMN withdrawal_frozen_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN users.withdrawal_frozen_at IS
'最高管理員啟用的提幣凍結時間；非空時，所有對外或對他人的資產轉出必須 fail closed，解除只能由受權限保護的管理端命令完成。';
