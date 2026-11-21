-- 合約風險政策與成交政策使用同一版本；發佈政策不代表開放合約下單。
-- HUMAN_REVIEW_REQUIRED：沒有已驗證的保證金、funding、強平與保險資金閉環，不得啟用商品。
-- 兌付池須與一般合約保證金池分離，避免引用錯誤資金責任。
ALTER TABLE accounts DROP CONSTRAINT ck_accounts_account_purpose;
ALTER TABLE accounts ADD CONSTRAINT ck_accounts_account_purpose CHECK (account_purpose IS NULL OR account_purpose IN
    ('AIRDROP_FUNDING','ASSET_ADJUSTMENT_COUNTERPARTY','WITHDRAWAL_CUSTODY','TRADING_FEE_REVENUE',
     'PERPETUAL_MARGIN_POOL','PERPETUAL_INSURANCE','PERPETUAL_CONVERSION_POOL'));

CREATE TABLE futures_risk_policies (
    instrument_id VARCHAR(64) NOT NULL,
    instrument_kind VARCHAR(16) NOT NULL DEFAULT 'PERPETUAL' CHECK (instrument_kind = 'PERPETUAL'),
    policy_version BIGINT NOT NULL CHECK (policy_version > 0),
    policy JSONB NOT NULL CHECK (jsonb_typeof(policy) = 'object'),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (instrument_id, policy_version),
    FOREIGN KEY (instrument_id, policy_version)
        REFERENCES trading_execution_policies(instrument_id, policy_version),
    FOREIGN KEY (instrument_id, instrument_kind)
        REFERENCES trading_instruments(instrument_id, instrument_kind)
);
COMMENT ON TABLE futures_risk_policies IS '不可變的逐倉風險政策；和成交政策同版，商品仍須另經 runtime gate 啟用。';
COMMENT ON COLUMN futures_risk_policies.policy IS '完整保證金、行情、funding、基金與兌付池邊界。';

CREATE TRIGGER immutable_futures_risk_policies BEFORE UPDATE OR DELETE ON futures_risk_policies
    FOR EACH ROW EXECUTE FUNCTION prevent_trading_evidence_mutation();
