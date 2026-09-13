-- P29-R10: 使用者可控制新裝置登入 email 通知，並保留既有裝置表作為目前綁定清單。
-- 預設開啟以維持既有帳戶的安全語意；關閉後未知裝置仍會建立可稽核的 session 與 device snapshot，
-- 但不再以 email 阻擋登入。此 migration 不涉及帳本、資產或任何資金移動。

ALTER TABLE users
    ADD COLUMN new_device_login_email_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE;

COMMENT ON COLUMN users.new_device_login_email_notification_enabled IS
'使用者的新裝置登入 email 通知開關；預設 TRUE 保留既有安全行為。TRUE 時 email Yes 會在確認瀏覽器建立一次性 session；FALSE 時未知裝置直接登入並建立受信任裝置。';
