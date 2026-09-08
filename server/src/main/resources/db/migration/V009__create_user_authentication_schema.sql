-- P29-R02: 使用者帳密、伺服器端 session 與密碼重設憑證。
-- 本 migration 僅保存認證資料；不接觸帳本、餘額、交易或任何資金移動。
-- 密碼與重設 token 只可保存不可逆雜湊，明文不得寫入資料庫、log 或 audit payload。

CREATE TABLE user_credentials (
    user_id VARCHAR(64) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL,
    password_algorithm VARCHAR(32) NOT NULL,
    password_changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_credentials_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_user_credentials_password_algorithm
        CHECK (password_algorithm = 'BCRYPT')
);

CREATE TABLE user_sessions (
    session_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_digest CHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_sessions_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_sessions_token_digest UNIQUE (token_digest),
    CONSTRAINT ck_user_sessions_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_user_sessions_active_lookup
    ON user_sessions (session_id, token_digest, expires_at)
    WHERE revoked_at IS NULL;

CREATE INDEX idx_user_sessions_user_id ON user_sessions (user_id);

CREATE TABLE password_reset_requests (
    reset_request_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    token_digest CHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_reset_requests_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_password_reset_requests_token_digest UNIQUE (token_digest),
    CONSTRAINT ck_password_reset_requests_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_password_reset_requests_active_lookup
    ON password_reset_requests (token_digest, expires_at)
    WHERE consumed_at IS NULL;

COMMENT ON TABLE user_credentials IS
'使用者認證資料。只保存 BCrypt 密碼雜湊與演算法資訊，禁止保存明文密碼。';
COMMENT ON COLUMN user_credentials.user_id IS
'所屬使用者，與 users.user_id 一對一。';
COMMENT ON COLUMN user_credentials.password_hash IS
'BCrypt 不可逆密碼雜湊；不得寫入明文、可逆加密結果或 log。';
COMMENT ON COLUMN user_credentials.password_algorithm IS
'雜湊演算法版本標示；目前僅允許 BCRYPT。';
COMMENT ON COLUMN user_credentials.password_changed_at IS
'最後一次成功變更密碼的時間，用於安全稽核與 session 失效判斷。';

COMMENT ON TABLE user_sessions IS
'伺服器端登入 session。Cookie 僅保存 session id 與高熵秘密值；資料庫只保存秘密值摘要。';
COMMENT ON COLUMN user_sessions.session_id IS
'非秘密的 session 識別碼，用於定位伺服器端 session。';
COMMENT ON COLUMN user_sessions.token_digest IS
'Cookie 高熵秘密值的 SHA-256 摘要；原始秘密值不落庫。';
COMMENT ON COLUMN user_sessions.expires_at IS
'session 絕對到期時間，逾期後不得再被認證。';
COMMENT ON COLUMN user_sessions.revoked_at IS
'撤銷時間；非空代表該 session 已永久失效。';

COMMENT ON TABLE password_reset_requests IS
'密碼重設一次性憑證。原始 token 只會出現在受控寄信內容，不得保存或回傳 API。';
COMMENT ON COLUMN password_reset_requests.token_digest IS
'一次性重設 token 的 SHA-256 摘要；原始 token 不得落庫。';
COMMENT ON COLUMN password_reset_requests.consumed_at IS
'成功重設密碼後的消耗時間；非空的 token 不得再次使用。';
