ALTER TABLE polymarket_session
    ADD COLUMN revoked_at VARCHAR(64) NULL,
    ADD COLUMN last_used_at VARCHAR(64) NULL,
    ADD COLUMN max_order_usdt DECIMAL(38, 18) NULL,
    ADD COLUMN daily_limit_usdt DECIMAL(38, 18) NULL,
    ADD COLUMN daily_used_usdt DECIMAL(38, 18) NULL,
    ADD COLUMN daily_reset_date VARCHAR(16) NULL,
    ADD COLUMN revoked_reason VARCHAR(256) NULL;
