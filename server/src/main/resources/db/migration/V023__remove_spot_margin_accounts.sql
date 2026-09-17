-- 現貨槓桿借貸不在產品範圍內；新帳戶容器僅保留 SPOT 與 FUTURES。
-- 不修改歷史 migration，並對任何已承載帳本或資產資料的 MARGIN 帳戶 fail closed，避免刪除稽核證據。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM accounts account
        WHERE account.account_type = 'MARGIN'
          AND (
              EXISTS (SELECT 1 FROM ledger_entries entry WHERE entry.account_id = account.account_id)
              OR EXISTS (SELECT 1 FROM balance_projections projection WHERE projection.account_id = account.account_id)
              OR EXISTS (SELECT 1 FROM reservations reservation WHERE reservation.account_id = account.account_id)
              OR EXISTS (SELECT 1 FROM orders orders_row WHERE orders_row.account_id = account.account_id)
              OR EXISTS (SELECT 1 FROM deposit_addresses address WHERE address.account_id = account.account_id)
              OR EXISTS (SELECT 1 FROM deposits deposit WHERE deposit.account_id = account.account_id)
              OR EXISTS (SELECT 1 FROM withdrawals withdrawal WHERE withdrawal.account_id = account.account_id)
          )
    ) THEN
        RAISE EXCEPTION 'cannot remove MARGIN accounts with dependent accounting or wallet evidence';
    END IF;
END $$;

DELETE FROM account_assets asset
USING accounts account
WHERE asset.account_id = account.account_id
  AND account.account_type = 'MARGIN';

DELETE FROM accounts WHERE account_type = 'MARGIN';

ALTER TABLE accounts DROP CONSTRAINT ck_accounts_type;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_type CHECK (account_type IN ('SPOT', 'FUTURES'));

COMMENT ON TABLE accounts IS
'使用者帳戶容器。僅區分 SPOT 與 FUTURES；本表不保存餘額，不處理資金異動。';

COMMENT ON COLUMN accounts.account_type IS
'帳戶類型。SPOT 為現貨帳戶；FUTURES 為合約帳戶。';
