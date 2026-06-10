-- Local first-party user authentication tables.
-- These tables intentionally store password hashes and refresh-token hashes only; raw secrets stay out of SQL.
CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Internal user id; also used as exchange account uid.',
    email VARCHAR(320) NOT NULL COMMENT 'Normalized lowercase first-party login identifier.',
    password_hash VARCHAR(255) NOT NULL COMMENT 'PBKDF2 password hash with algorithm metadata and salt.',
    status VARCHAR(32) NOT NULL COMMENT 'User lifecycle state such as ACTIVE or future disabled states.',
    roles VARCHAR(255) NOT NULL COMMENT 'Space-delimited roles copied into issued JWT claims.',
    scopes VARCHAR(255) NOT NULL COMMENT 'Space-delimited scopes copied into issued JWT claims.',
    created_at DATETIME(6) NOT NULL COMMENT 'Registration timestamp.',
    updated_at DATETIME(6) NOT NULL COMMENT 'Last profile/status update timestamp.',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_users_email (email),
    KEY idx_app_users_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='First-party exchange users for local registration and login.';

-- Refresh sessions make logout/revocation server-side while access JWTs remain short-lived and stateless.
CREATE TABLE IF NOT EXISTS auth_refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Refresh session primary key.',
    user_id BIGINT NOT NULL COMMENT 'Owner app_users.id / exchange uid.',
    refresh_token_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hex of refresh token; raw token is never stored.',
    session_id VARCHAR(64) NOT NULL COMMENT 'Stable session identifier for future device/session management.',
    expires_at DATETIME(6) NOT NULL COMMENT 'Hard expiry for refresh-token reuse.',
    revoked_at DATETIME(6) COMMENT 'Logout/revocation timestamp; null means still active until expiry.',
    created_at DATETIME(6) NOT NULL COMMENT 'Session creation timestamp.',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_refresh_sessions_token_hash (refresh_token_hash),
    KEY idx_auth_refresh_sessions_user_created (user_id, created_at),
    KEY idx_auth_refresh_sessions_expires (expires_at),
    CONSTRAINT fk_auth_refresh_sessions_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Server-side refresh-token sessions for local exchange auth.';
