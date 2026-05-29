-- Durable idempotency and lifecycle tracking for backend-observed RPC transactions.
CREATE TABLE IF NOT EXISTS rpc_transaction_record (
    command_id VARCHAR(128) PRIMARY KEY,
    chain_id VARCHAR(32) NOT NULL,
    transaction_type VARCHAR(64) NOT NULL,
    wallet_address VARCHAR(64) NOT NULL,
    fingerprint VARCHAR(512) NOT NULL,
    tx_hash VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    last_error TEXT NULL,
    completed BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_rpc_tx_hash (tx_hash),
    KEY idx_rpc_tx_wallet (wallet_address),
    KEY idx_rpc_tx_type_completed (transaction_type, completed),
    KEY idx_rpc_tx_status_updated (status, updated_at)
);
