-- 外部送單 Proxy 的 runtime 開關保存於資料庫；Demo 結果仍不建立正式訂單、成交或帳本資料。
CREATE TABLE external_order_proxy_settings (
    setting_key VARCHAR(64) PRIMARY KEY
        CHECK (setting_key = 'LUMIX_TRADING_EXTERNAL_PROXY_DEMO_ENABLED'),
    enabled BOOLEAN NOT NULL,
    version BIGINT NOT NULL CHECK (version > 0),
    updated_by VARCHAR(64) REFERENCES users(user_id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO external_order_proxy_settings(setting_key, enabled, version, updated_by)
VALUES ('LUMIX_TRADING_EXTERNAL_PROXY_DEMO_ENABLED', TRUE, 1, NULL);

COMMENT ON TABLE external_order_proxy_settings IS '外部送單 Proxy 的資料庫 runtime 開關；變更不需重啟服務。';
COMMENT ON COLUMN external_order_proxy_settings.setting_key IS '穩定設定鍵，目前只允許 Demo provider 開關。';
COMMENT ON COLUMN external_order_proxy_settings.enabled IS 'true 時允許 loopback Demo API 與 DEMO provider；初始值為 true。';
COMMENT ON COLUMN external_order_proxy_settings.version IS '每次變更遞增，用於避免管理命令覆蓋較新設定。';
COMMENT ON COLUMN external_order_proxy_settings.updated_by IS '最後變更設定的最高管理員；migration 初始值為 NULL。';
COMMENT ON COLUMN external_order_proxy_settings.updated_at IS '最後成功變更時間。';
