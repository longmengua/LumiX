-- 補齊提款 runtime 的廣播不確定性與鏈上失敗證據。只延伸既有提款／nonce／鏈交易資料，絕不建立第二個提款主檔。
-- 回滾：不可直接刪除證據；應先停用 worker、保全鏈上與帳本稽核資料，再由人工決定資料修復程序。

ALTER TABLE withdrawals DROP CONSTRAINT ck_withdrawals_status;
ALTER TABLE withdrawals ADD CONSTRAINT ck_withdrawals_status CHECK (status IN (
    'REQUESTED','FUNDS_HELD','RISK_PENDING','RISK_HOLD','MANUAL_REVIEW','APPROVED','REJECTED',
    'FINAL_RISK_CHECK','SIGN_PENDING','SIGNING','SIGNED','BROADCASTING','BROADCAST_UNKNOWN',
    'BROADCASTED','CONFIRMING','COMPLETED','FAILED','CANCELLED'
));

ALTER TABLE withdrawal_nonce_allocations
    ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'RESERVED',
    ADD COLUMN transaction_hash CHAR(66),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT ck_withdrawal_nonce_allocation_status CHECK (status IN ('RESERVED','SIGNED','BROADCAST_UNKNOWN','BROADCASTED','CHAIN_FAILED','SETTLED'));
CREATE UNIQUE INDEX uq_withdrawal_nonce_allocation_tx_hash ON withdrawal_nonce_allocations(transaction_hash) WHERE transaction_hash IS NOT NULL;

ALTER TABLE withdrawal_chain_transactions
    ADD COLUMN gas_used NUMERIC(38,0),
    ADD COLUMN effective_gas_price NUMERIC(38,0),
    ADD COLUMN chain_failure_recorded_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN withdrawal_nonce_allocations.status IS 'nonce 的不可倒退 lifecycle；BROADCAST_UNKNOWN 前不可重簽或釋放 principal hold。';
COMMENT ON COLUMN withdrawal_chain_transactions.gas_used IS '失敗或成功收據的 gasUsed 證據；不作 principal 轉出成功的判定。';
