-- 檔案用途：SQL migration，建立做市商 hedge decision audit trail。
CREATE TABLE IF NOT EXISTS hedge_decision_audits (
    id CHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    market_maker_id VARCHAR(128) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    accepted BOOLEAN NOT NULL,
    reason VARCHAR(128) NULL,
    order_notional DECIMAL(38, 18) NOT NULL,
    venue_order_id VARCHAR(128) NULL,
    ref_id VARCHAR(128) NULL,
    decided_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_hedge_decision_mm_time (market_maker_id, decided_at),
    KEY idx_hedge_decision_symbol_time (symbol, decided_at),
    KEY idx_hedge_decision_ref (ref_id),
    KEY idx_hedge_decision_accepted (accepted, decided_at),
    CONSTRAINT chk_hedge_decision_notional_non_negative CHECK (order_notional >= 0)
);
