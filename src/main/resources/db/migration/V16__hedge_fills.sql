-- 檔案用途：SQL migration，建立做市商 hedge fill audit trail。
CREATE TABLE IF NOT EXISTS hedge_fills (
    id CHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    market_maker_id VARCHAR(128) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    venue_order_id VARCHAR(128) NOT NULL,
    venue_fill_id VARCHAR(128) NOT NULL,
    side VARCHAR(16) NOT NULL,
    quantity DECIMAL(38, 18) NOT NULL,
    price DECIMAL(38, 18) NOT NULL,
    fee DECIMAL(38, 18) NOT NULL DEFAULT 0,
    fee_asset VARCHAR(32) NULL,
    ref_id VARCHAR(128) NULL,
    filled_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_hedge_fills_venue_fill (venue_order_id, venue_fill_id),
    KEY idx_hedge_fills_mm_time (market_maker_id, filled_at),
    KEY idx_hedge_fills_venue_order (venue_order_id),
    KEY idx_hedge_fills_ref (ref_id),
    KEY idx_hedge_fills_symbol_time (symbol, filled_at),
    CONSTRAINT chk_hedge_fills_non_negative CHECK (
        quantity > 0
        AND price > 0
        AND fee >= 0
    )
);
