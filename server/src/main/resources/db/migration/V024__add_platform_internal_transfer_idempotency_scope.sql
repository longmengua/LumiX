-- 平台內部轉出必須使用獨立 idempotency scope，避免與同使用者帳戶劃轉的重送鍵混用。
-- rollback：確認所有 PLATFORM_INTERNAL_TRANSFER 請求均已結束且沒有可重送請求後，才可移除本 scope。
ALTER TABLE idempotency_keys DROP CONSTRAINT ck_idempotency_keys_scope;
ALTER TABLE idempotency_keys ADD CONSTRAINT ck_idempotency_keys_scope CHECK (scope IN ('ORDER_CREATE', 'ORDER_CANCEL', 'WITHDRAWAL_REQUEST', 'WITHDRAWAL_CANCEL', 'LEDGER_POSTING', 'DEPOSIT_CONFIRMATION', 'RESERVATION_HOLD', 'RESERVATION_RELEASE', 'RESERVATION_CAPTURE', 'INTERNAL_TRANSFER', 'PLATFORM_INTERNAL_TRANSFER', 'ADMIN_ACTION'));

COMMENT ON COLUMN idempotency_keys.scope IS '高風險操作的 idempotency 範圍；平台內部轉出與帳戶間劃轉必須分離，避免跨流程回放。';
