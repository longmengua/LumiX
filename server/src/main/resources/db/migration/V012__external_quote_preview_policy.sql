-- 只保存外部報價預覽政策版本；不啟用 Binance 對沖、客戶成交或資金異動。
CREATE TABLE external_quote_preview_policies (
    policy_version BIGINT PRIMARY KEY CHECK (policy_version > 0),
    source VARCHAR(32) NOT NULL CHECK (source = 'BINANCE'),
    policy JSONB NOT NULL CHECK (jsonb_typeof(policy) = 'object'),
    created_by VARCHAR(64) NOT NULL REFERENCES users(user_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE external_quote_preview_policies IS '最高管理員版本化的外部報價預覽設定；沒有交易啟用效力。';
COMMENT ON COLUMN external_quote_preview_policies.policy_version IS '嚴格遞增版本；同版內容不可覆寫。';
COMMENT ON COLUMN external_quote_preview_policies.source IS '預覽行情來源，目前固定 BINANCE。';
COMMENT ON COLUMN external_quote_preview_policies.policy IS '原始十進位字串費率與幣別調整，供明確重算及稽核。';
COMMENT ON COLUMN external_quote_preview_policies.created_by IS '提交政策的最高管理員。';
COMMENT ON COLUMN external_quote_preview_policies.created_at IS '政策寫入時間；不代表交易開始時間。';
