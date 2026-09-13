-- P27 管理者啟用基礎：最高管理員是獨立 principal，不可用前端 localStorage 或環境信箱白名單取代。
-- 本 migration 只建立單一最高管理員的身分與啟用狀態；不提供角色指派、後台寫入命令、資金或帳本操作。

CREATE TABLE admin_principals (
    -- 主體沿用 users 的受驗證身分，避免另建一套密碼或 session 儲存機制。
    user_id VARCHAR(64) PRIMARY KEY,

    -- 目前只允許唯一且不可由 runtime 任意變更的最高管理員角色。
    role VARCHAR(32) NOT NULL,

    -- PENDING_ACTIVATION 在 email 設定密碼前沒有任何後台權限；ACTIVE 才能通過 server-side 授權。
    status VARCHAR(32) NOT NULL,

    -- 只記錄啟用流程時間，不保存 email token、明文密碼或可逆祕密。
    activation_requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_admin_principals_user_id
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_admin_principals_role UNIQUE (role),
    CONSTRAINT ck_admin_principals_role CHECK (role = 'SUPER_ADMIN'),
    CONSTRAINT ck_admin_principals_status CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE')),
    CONSTRAINT ck_admin_principals_activation_time
        CHECK ((status = 'PENDING_ACTIVATION' AND activated_at IS NULL)
            OR (status = 'ACTIVE' AND activated_at IS NOT NULL))
);

CREATE INDEX idx_admin_principals_active_user
    ON admin_principals (user_id)
    WHERE status = 'ACTIVE';

COMMENT ON TABLE admin_principals IS
'管理後台 principal。僅保存最高管理員啟用狀態；不得用於一般角色授與或任何資金操作。';
COMMENT ON COLUMN admin_principals.user_id IS
'最高管理員對應的既有 users 身分；密碼仍只存於 user_credentials 的 BCrypt 雜湊。';
COMMENT ON COLUMN admin_principals.role IS
'角色固定為 SUPER_ADMIN，unique 約束阻止啟動設定靜默新增或取代第二位最高管理員。';
COMMENT ON COLUMN admin_principals.status IS
'PENDING_ACTIVATION 不具後台權限；只有成功使用 email 一次性連結設定密碼後才會成為 ACTIVE。';
COMMENT ON COLUMN admin_principals.activation_requested_at IS
'最近一次啟用信成功排程的時間；不保存信件內的一次性 token。';
COMMENT ON COLUMN admin_principals.activated_at IS
'首次成功設定最高管理員密碼並啟用 server-side 授權的時間。';
