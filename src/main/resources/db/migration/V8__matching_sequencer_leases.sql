-- 檔案用途：SQL migration，建立 matching sequencer lease / epoch fencing baseline。
CREATE TABLE IF NOT EXISTS matching_sequencer_leases (
    symbol_code VARCHAR(32) PRIMARY KEY,
    owner_id VARCHAR(128) NOT NULL,
    epoch BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    command_offset BIGINT NOT NULL DEFAULT 0,
    event_offset BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    KEY idx_matching_sequencer_owner (owner_id, expires_at),
    KEY idx_matching_sequencer_expiry (expires_at)
);
