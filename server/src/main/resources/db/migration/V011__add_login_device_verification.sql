-- P29-R08: 受信任裝置、新裝置登入確認與成功登入的裝置快照。
-- 此 migration 只強化認證邊界；不接觸帳本、資產、交易或任何資金移動。
-- 所有 cookie／email token 均只保存 SHA-256 摘要，原始秘密不可落庫、寫入 log 或回傳 API。

CREATE TABLE user_login_devices (
    device_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_digest CHAR(64) NOT NULL,
    user_agent_digest CHAR(64) NOT NULL,
    device_label VARCHAR(256) NOT NULL,
    last_ip_address VARCHAR(64) NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_user_login_devices_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_login_devices_token_digest UNIQUE (token_digest)
);

CREATE INDEX idx_user_login_devices_active_lookup
    ON user_login_devices (user_id, device_id, token_digest)
    WHERE revoked_at IS NULL;

CREATE TABLE login_verification_requests (
    verification_request_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    pending_token_digest CHAR(64) NOT NULL,
    approval_token_digest CHAR(64) NOT NULL,
    candidate_device_id UUID NOT NULL,
    candidate_device_token_digest CHAR(64) NOT NULL,
    user_agent_digest CHAR(64) NOT NULL,
    device_label VARCHAR(256) NOT NULL,
    ip_address VARCHAR(64) NOT NULL,
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_at TIMESTAMP WITH TIME ZONE,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_login_verification_requests_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_login_verification_requests_pending_digest UNIQUE (pending_token_digest),
    CONSTRAINT uq_login_verification_requests_approval_digest UNIQUE (approval_token_digest),
    CONSTRAINT ck_login_verification_requests_state
        CHECK (state IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_login_verification_requests_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_login_verification_requests_pending_lookup
    ON login_verification_requests (verification_request_id, pending_token_digest, expires_at)
    WHERE consumed_at IS NULL;

CREATE INDEX idx_login_verification_requests_approval_lookup
    ON login_verification_requests (approval_token_digest, expires_at)
    WHERE consumed_at IS NULL;

ALTER TABLE user_sessions
    ADD COLUMN device_id UUID,
    ADD COLUMN ip_address VARCHAR(64),
    ADD COLUMN device_label VARCHAR(256);

CREATE INDEX idx_user_sessions_login_history_device
    ON user_sessions (user_id, created_at DESC);

COMMENT ON TABLE user_login_devices IS
'已由 email 明確確認的登入裝置。Cookie 原始值只在 HttpOnly browser cookie，資料庫只保存不可逆摘要。';
COMMENT ON COLUMN user_login_devices.token_digest IS
'受信任裝置 Cookie 的高熵秘密摘要；不得保存原始 cookie 值。';
COMMENT ON COLUMN user_login_devices.user_agent_digest IS
'登入時 User-Agent 的 SHA-256 摘要；變更時須重新進行 email 裝置確認。';
COMMENT ON COLUMN user_login_devices.last_ip_address IS
'最後一次以此受信任裝置登入時由可信反向代理轉交的用戶端 IP。';

COMMENT ON TABLE login_verification_requests IS
'新裝置登入的一次性 email 確認請求。核准只允許原始登入瀏覽器以 pending cookie 交換 session。';
COMMENT ON COLUMN login_verification_requests.pending_token_digest IS
'原始登入瀏覽器 HttpOnly pending cookie 的秘密摘要；只可完成同一次登入。';
COMMENT ON COLUMN login_verification_requests.approval_token_digest IS
'email 確認頁使用的一次性 token 摘要；原始 token 不可寫入 log、DB 或 API response。';
COMMENT ON COLUMN login_verification_requests.candidate_device_token_digest IS
'核准後建立受信任裝置時使用的 cookie 秘密摘要；核准前不可視為可信裝置。';
COMMENT ON COLUMN login_verification_requests.state IS
'PENDING 等待使用者決定；APPROVED 只允許原始登入瀏覽器完成一次；REJECTED 永不建立 session。';
COMMENT ON COLUMN login_verification_requests.consumed_at IS
'成功以核准請求建立 session 後的消耗時間；非空表示不得再完成登入。';

COMMENT ON COLUMN user_sessions.device_id IS
'建立此成功 session 的受信任裝置識別；歷史 session 可為空，不能藉 migration 偽造裝置資料。';
COMMENT ON COLUMN user_sessions.ip_address IS
'成功登入時由應用程式取得的用戶端 IP 快照，僅供本人登入紀錄與安全通知使用。';
COMMENT ON COLUMN user_sessions.device_label IS
'成功登入時的去敏裝置標籤快照，避免在登入歷程保存原始 User-Agent。';
