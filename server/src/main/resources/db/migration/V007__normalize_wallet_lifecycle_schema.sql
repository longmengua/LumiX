-- P12-T07: Normalize wallet lifecycle schema without changing the V005 table definitions.
-- 這份 migration 只補強查詢索引，讓 support、reconciliation 與營運查詢能更穩定地查到 wallet lifecycle 資料。
-- 不修改 V005 的欄位、約束或 comment，不新增 runtime service，也不改變任何入金／提款流程語意。
-- Rollback 注意事項：若要回滾，僅移除本 migration 新增的索引即可；不得回頭改寫 V005。

-- 入金地址與入金紀錄通常會被 support 以 address 直接查詢，因此補 address 層級索引。
CREATE INDEX idx_deposit_addresses_address ON deposit_addresses (address);
CREATE INDEX idx_deposits_chain_type_address ON deposits (chain_type, address);

-- 提現紀錄也需要以外部地址與鏈別做查詢，避免 support 與對帳只能依賴 request_id 或 account_id。
CREATE INDEX idx_withdrawals_chain_type_address ON withdrawals (chain_type, address);

-- 鏈上交易通常以 tx_hash 直接回查，因此補單欄位索引，降低 support / reconciliation 的查詢成本。
CREATE INDEX idx_chain_transactions_tx_hash ON chain_transactions (tx_hash);
