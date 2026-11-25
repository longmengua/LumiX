-- 現貨與合約總開關保存於資料庫，讓管理命令可即時切換且不依賴程序環境變數。
CREATE TABLE trading_runtime_settings (
    trading_kind VARCHAR(16) PRIMARY KEY
        CHECK (trading_kind IN ('SPOT', 'FUTURES')),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL CHECK (version > 0),
    updated_by VARCHAR(64) REFERENCES users(user_id),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO trading_runtime_settings (trading_kind, enabled, version, updated_by) VALUES
    ('SPOT', FALSE, 1, NULL),
    ('FUTURES', FALSE, 1, NULL);

COMMENT ON TABLE trading_runtime_settings IS '現貨與合約的資料庫權威總開關；預設關閉，變更無須重啟服務。';
COMMENT ON COLUMN trading_runtime_settings.trading_kind IS '穩定交易類型，只允許 SPOT 或 FUTURES。';
COMMENT ON COLUMN trading_runtime_settings.enabled IS 'false 時拒絕新單，但仍允許安全撤單。';
COMMENT ON COLUMN trading_runtime_settings.version IS '樂觀鎖版本，每次管理變更必須遞增。';
