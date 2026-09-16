-- 內部劃轉須以自己的 durable idempotency scope 保護 reservation、ledger append 與餘額投影的整體 transaction。
ALTER TABLE idempotency_keys DROP CONSTRAINT ck_idempotency_keys_scope;
ALTER TABLE idempotency_keys ADD CONSTRAINT ck_idempotency_keys_scope CHECK (scope IN ('ORDER_CREATE', 'ORDER_CANCEL', 'WITHDRAWAL_REQUEST', 'WITHDRAWAL_CANCEL', 'LEDGER_POSTING', 'DEPOSIT_CONFIRMATION', 'RESERVATION_HOLD', 'RESERVATION_RELEASE', 'RESERVATION_CAPTURE', 'INTERNAL_TRANSFER', 'ADMIN_ACTION'));
COMMENT ON COLUMN idempotency_keys.scope IS '高風險操作的 idempotency 範圍；內部劃轉必須與 reservation 與 ledger posting 分離。';
