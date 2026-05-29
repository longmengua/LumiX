-- Durable market ticker latest state for restart-safe top/last/24h fields.
CREATE TABLE IF NOT EXISTS market_data_tickers (
    symbol VARCHAR(32) PRIMARY KEY,
    last_price DECIMAL(38, 18) NULL,
    best_bid DECIMAL(38, 18) NULL,
    best_ask DECIMAL(38, 18) NULL,
    volume_24h DECIMAL(38, 18) NOT NULL,
    high_24h DECIMAL(38, 18) NULL,
    low_24h DECIMAL(38, 18) NULL,
    updated_at DATETIME(6) NOT NULL
);
