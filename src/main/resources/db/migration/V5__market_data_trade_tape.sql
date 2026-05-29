-- Durable market-data trade tape for restart-safe recent trades.
CREATE TABLE IF NOT EXISTS market_data_trade_tape (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    match_id VARCHAR(128) NOT NULL,
    order_id VARCHAR(36) NULL,
    side VARCHAR(16) NOT NULL,
    price DECIMAL(38, 18) NOT NULL,
    qty DECIMAL(38, 18) NOT NULL,
    maker BOOLEAN NOT NULL,
    trade_ts DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_md_trade_symbol_time (symbol, trade_ts, id),
    KEY idx_md_trade_match (match_id)
);
