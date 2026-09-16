-- reservation capture 是獨立於 hold/release 的不可重送資金控制動作，必須有自己的 idempotency scope。
ALTER TABLE idempotency_keys DROP CONSTRAINT ck_idempotency_keys_scope;
ALTER TABLE idempotency_keys ADD CONSTRAINT ck_idempotency_keys_scope CHECK (scope IN ('ORDER_CREATE', 'ORDER_CANCEL', 'WITHDRAWAL_REQUEST', 'WITHDRAWAL_CANCEL', 'LEDGER_POSTING', 'DEPOSIT_CONFIRMATION', 'RESERVATION_HOLD', 'RESERVATION_RELEASE', 'RESERVATION_CAPTURE', 'ADMIN_ACTION'));
COMMENT ON COLUMN idempotency_keys.scope IS '高風險操作的 idempotency 範圍；reservation capture 與 hold/release 必須分離，避免跨操作回放。';
