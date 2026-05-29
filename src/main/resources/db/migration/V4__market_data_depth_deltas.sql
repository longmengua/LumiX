-- Durable depth deltas for reconnect backfill after a known client sequence.
CREATE TABLE IF NOT EXISTS market_data_depth_deltas (
    symbol VARCHAR(32) NOT NULL,
    version_value BIGINT NOT NULL,
    checksum BIGINT NOT NULL,
    bids_json LONGTEXT NOT NULL,
    asks_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (symbol, version_value),
    KEY idx_md_depth_symbol_version (symbol, version_value)
);
