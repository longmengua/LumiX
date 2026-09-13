-- P29-R12: 每位使用者各只保留一台桌電、平板與手機綁定裝置。
-- 裝置類別僅是由 User-Agent 推導的產品槽位，不能替代 cookie 秘密、摘要比對與 email 核准。

ALTER TABLE user_login_devices
    ADD COLUMN device_platform VARCHAR(16);

-- 舊資料沒有完整 UA 可重新分類，只能由去敏標籤做保守一次性歸類；未知資料放桌電槽位，不建立可繞過限制的類別。
UPDATE user_login_devices
SET device_platform = CASE
    WHEN LOWER(device_label) LIKE '%ipad%' THEN 'TABLET'
    WHEN LOWER(device_label) LIKE '%tablet%' THEN 'TABLET'
    WHEN LOWER(device_label) LIKE '%iphone%' OR LOWER(device_label) LIKE '%ipod%' OR LOWER(device_label) LIKE '%android%' THEN 'MOBILE'
    ELSE 'DESKTOP'
END;

ALTER TABLE user_login_devices
    ALTER COLUMN device_platform SET NOT NULL,
    ADD CONSTRAINT ck_user_login_devices_platform
        CHECK (device_platform IN ('DESKTOP', 'TABLET', 'MOBILE'));

-- 套用一槽一台前，保留每個類別最後使用的一台；其餘既有 device 與所屬 active session 一起撤銷，不能留下幽靈登入。
WITH ranked_devices AS (
    SELECT device_id,
        ROW_NUMBER() OVER (PARTITION BY user_id, device_platform ORDER BY last_seen_at DESC, device_id DESC) AS position
    FROM user_login_devices
    WHERE revoked_at IS NULL
), superseded_devices AS (
    SELECT device_id FROM ranked_devices WHERE position > 1
)
UPDATE user_sessions
SET revoked_at = CURRENT_TIMESTAMP
WHERE revoked_at IS NULL
  AND device_id IN (SELECT device_id FROM superseded_devices);

WITH ranked_devices AS (
    SELECT device_id,
        ROW_NUMBER() OVER (PARTITION BY user_id, device_platform ORDER BY last_seen_at DESC, device_id DESC) AS position
    FROM user_login_devices
    WHERE revoked_at IS NULL
)
UPDATE user_login_devices
SET revoked_at = CURRENT_TIMESTAMP
WHERE device_id IN (SELECT device_id FROM ranked_devices WHERE position > 1);

CREATE UNIQUE INDEX uq_user_login_devices_active_platform
    ON user_login_devices (user_id, device_platform)
    WHERE revoked_at IS NULL;

ALTER TABLE login_verification_requests
    ADD COLUMN device_platform VARCHAR(16) NOT NULL DEFAULT 'DESKTOP',
    ADD CONSTRAINT ck_login_verification_requests_platform
        CHECK (device_platform IN ('DESKTOP', 'TABLET', 'MOBILE'));

ALTER TABLE login_verification_requests
    ALTER COLUMN device_platform DROP DEFAULT;

COMMENT ON COLUMN user_login_devices.device_platform IS
'帳戶受限裝置槽位：每位使用者每個 DESKTOP、TABLET、MOBILE 類別只有一台 active device。';
COMMENT ON COLUMN login_verification_requests.device_platform IS
'原始登入候選裝置的受限槽位；email Yes 只核准此候選裝置，不綁定閱讀 email 的其他瀏覽器。';
COMMENT ON COLUMN users.new_device_login_email_notification_enabled IS
'歷史通知偏好欄位；V014 起不再參與登入決策，平台換機一律要求 email 核准。';
