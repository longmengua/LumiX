-- Durable ADL queue state for liquidation shortfalls and operator claim ownership.
CREATE TABLE IF NOT EXISTS adl_queue_entries (
    liquidation_id VARCHAR(128) PRIMARY KEY,
    uid BIGINT NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    liquidated_side VARCHAR(16) NOT NULL,
    amount DECIMAL(38, 18) NOT NULL,
    status VARCHAR(32) NOT NULL,
    owner VARCHAR(128) NOT NULL,
    claimed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_adl_queue_status_time (status, created_at),
    KEY idx_adl_queue_owner_status (owner, status),
    KEY idx_adl_queue_symbol_status (symbol, status)
);
