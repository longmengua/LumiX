-- 檔案用途：SQL migration，建立做市商 profile 與 per-symbol risk limit。
CREATE TABLE IF NOT EXISTS market_maker_profiles (
    market_maker_id VARCHAR(128) PRIMARY KEY,
    schema_version INT NOT NULL,
    uid BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    KEY idx_market_maker_profiles_uid (uid),
    KEY idx_market_maker_profiles_enabled (enabled),
    CONSTRAINT uk_market_maker_profiles_uid UNIQUE (uid)
);

CREATE TABLE IF NOT EXISTS market_maker_risk_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    market_maker_id VARCHAR(128) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    max_long_notional DECIMAL(38, 18) NOT NULL DEFAULT 0,
    max_short_notional DECIMAL(38, 18) NOT NULL DEFAULT 0,
    max_order_notional DECIMAL(38, 18) NOT NULL DEFAULT 0,
    max_slippage_rate DECIMAL(38, 18) NOT NULL DEFAULT 0,
    kill_switch BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE KEY uk_market_maker_risk_limit_symbol (market_maker_id, symbol),
    KEY idx_market_maker_risk_limits_mm (market_maker_id),
    CONSTRAINT fk_market_maker_risk_limits_profile
        FOREIGN KEY (market_maker_id) REFERENCES market_maker_profiles (market_maker_id),
    CONSTRAINT chk_market_maker_risk_limit_non_negative CHECK (
        max_long_notional >= 0
        AND max_short_notional >= 0
        AND max_order_notional >= 0
        AND max_slippage_rate >= 0
    )
);
