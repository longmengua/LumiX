-- 檔案用途：SQL migration，建立體驗金 grant 批次 read model。
CREATE TABLE IF NOT EXISTS bonus_credit_grants (
    id CHAR(36) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    asset VARCHAR(32) NOT NULL,
    original_amount DECIMAL(38, 18) NOT NULL,
    remaining_amount DECIMAL(38, 18) NOT NULL,
    campaign_id VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    granted_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_bonus_credit_uid_asset_status (uid, asset, status, expires_at),
    KEY idx_bonus_credit_expiry (status, expires_at),
    KEY idx_bonus_credit_campaign (campaign_id),
    CONSTRAINT chk_bonus_credit_original_non_negative CHECK (original_amount >= 0),
    CONSTRAINT chk_bonus_credit_remaining_non_negative CHECK (remaining_amount >= 0)
);
