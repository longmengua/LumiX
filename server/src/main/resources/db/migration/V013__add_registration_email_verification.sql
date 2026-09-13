-- P29-R11: 註冊必須完成 email 雙驗證碼後才建立 users / credential / session。
-- 原始數字碼與五碼英文字母碼絕不可落庫；只保存 SHA-256 摘要。密碼只以 BCrypt hash 暫存，驗證過期或
-- 錯誤次數用盡時不會建立帳號。本 migration 不涉及帳本、資產、交易或任何資金移動。

CREATE TABLE registration_verification_requests (
    registration_id UUID PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    numeric_code_digest CHAR(64) NOT NULL,
    letter_code_digest CHAR(64) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_registration_verification_requests_email UNIQUE (email),
    CONSTRAINT ck_registration_verification_requests_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_registration_verification_requests_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_registration_verification_requests_active_lookup
    ON registration_verification_requests (registration_id, expires_at)
    WHERE consumed_at IS NULL;

COMMENT ON TABLE registration_verification_requests IS
'尚未完成 email 雙驗證碼的註冊申請。驗證成功前不得建立 users、user_credentials、受信任裝置或 session。';
COMMENT ON COLUMN registration_verification_requests.password_hash IS
'註冊時已計算的 BCrypt 密碼雜湊；原始密碼不得在此表、log 或 API response 出現。';
COMMENT ON COLUMN registration_verification_requests.numeric_code_digest IS
'六位數 email 驗證碼的 SHA-256 摘要；原始碼只存在於寄送中的信件。';
COMMENT ON COLUMN registration_verification_requests.letter_code_digest IS
'五位英文字母 email 驗證碼的 SHA-256 摘要；原始碼只存在於寄送中的信件。';
COMMENT ON COLUMN registration_verification_requests.attempt_count IS
'已驗證失敗次數；達到 application 設定上限時會標記 consumed，使用者必須重新發起註冊。';
