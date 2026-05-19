-- 檔案用途：SQL migration，建立 account risk snapshot 持久化表。
CREATE TABLE IF NOT EXISTS account_risk_snapshots (
    id CHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    cross_balance DECIMAL(38, 18) NOT NULL,
    available_balance DECIMAL(38, 18) NOT NULL,
    order_hold DECIMAL(38, 18) NOT NULL,
    position_margin DECIMAL(38, 18) NOT NULL,
    frozen_funds DECIMAL(38, 18) NOT NULL,
    unrealized_pnl DECIMAL(38, 18) NOT NULL,
    total_equity DECIMAL(38, 18) NOT NULL,
    maintenance_margin DECIMAL(38, 18) NOT NULL,
    risk_ratio DECIMAL(38, 18) NOT NULL,
    open_position_count INT NOT NULL,
    calculated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    KEY idx_account_risk_snapshots_uid_day (uid, snapshot_date),
    KEY idx_account_risk_snapshots_uid_time (uid, calculated_at),
    CONSTRAINT chk_account_risk_snapshot_open_positions CHECK (open_position_count >= 0)
);
