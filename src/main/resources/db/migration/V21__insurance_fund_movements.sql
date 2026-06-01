-- Durable insurance fund capital movement records for liquidation/ADL operations.
CREATE TABLE IF NOT EXISTS insurance_fund_movements (
    movement_id VARCHAR(128) PRIMARY KEY,
    asset VARCHAR(32) NOT NULL,
    reason VARCHAR(128) NOT NULL,
    ref_id VARCHAR(128) NOT NULL,
    amount DECIMAL(38, 18) NOT NULL,
    balance_after DECIMAL(38, 18) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_insurance_fund_asset_time (asset, created_at),
    KEY idx_insurance_fund_reason_time (reason, created_at),
    KEY idx_insurance_fund_ref (ref_id)
);
