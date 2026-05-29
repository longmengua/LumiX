-- Durable market kline records for restart-safe candlestick queries.
CREATE TABLE IF NOT EXISTS market_data_klines (
    symbol VARCHAR(32) NOT NULL,
    interval_value VARCHAR(16) NOT NULL,
    open_time DATETIME(6) NOT NULL,
    open_price DECIMAL(38, 18) NOT NULL,
    high_price DECIMAL(38, 18) NOT NULL,
    low_price DECIMAL(38, 18) NOT NULL,
    close_price DECIMAL(38, 18) NOT NULL,
    volume DECIMAL(38, 18) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (symbol, interval_value, open_time),
    KEY idx_md_kline_symbol_interval_time (symbol, interval_value, open_time)
);
