# P29-R10：新裝置通知偏好與 email 確認頁直接登入

## 任務目的

讓帳戶本人可在個人中心決定未知裝置登入是否需要 email Yes／No 通知，同時在核准後直接登入開啟確認頁的瀏覽器，不要求使用者返回原始登入分頁。這是 authentication／authorization runtime 變更，不是 MFA、風控、SIEM 或 production launch 宣告。

## 完成內容

- `V012` append-only migration 在 `users` 增加 `new_device_login_email_notification_enabled`，預設 `TRUE`，維持既有帳戶的安全行為。
- `GET /api/v1/account/security` 只回傳目前 session owner 的通知偏好與有效綁定裝置清單；response 不含 cookie、token、digest 或完整 User-Agent。
- `PATCH /api/v1/account/security/new-device-email-notification` 只允許目前登入者開關新裝置 email 通知，不接受 userId。
- `DELETE /api/v1/account/security/devices/{deviceId}` 只允許目前登入者撤銷自己的綁定裝置，並同步撤銷該裝置所有 active session；不是只把資料從畫面隱藏。
- 開關為 `FALSE` 時，未知裝置會直接建立受信任 device cookie、server-side session 與登入歷程；不會寄送通知或遺失裝置 evidence。開關為 `TRUE` 但尚無任何綁定裝置時，也會直接建立第一個裝置；沒有既有安全基線時不寄送無意義的「新裝置」通知。
- 開關為 `TRUE` 時，email Yes 確認頁以 POST 原子消耗短效 approval token，並在確認頁所在瀏覽器核發 HttpOnly session/device cookie；No 永遠不建立 session。
- 每次成功 Yes 都建立新的受信任裝置與成功登入 session，因此「目前綁定裝置」與登入紀錄會保留該次核准 evidence。
- 個人中心安全頁已接上真實通知開關與裝置清單，不以 mock 資料替代。

## 安全不變式

```text
未知裝置 + 設定開啟 + 已有綁定裝置
    |
    +-- SMTP email（IP、裝置、時間、短效 token）
             |
             +-- GET 確認頁：不改變狀態
             |
             +-- POST Yes --原子消耗 token--> 確認頁 browser 的 device + session
             |
             +-- POST No  -------------------> REJECTED，沒有 session

未知裝置 + 設定關閉，或設定開啟但裝置清單為空
    |
    +-- 建立 device + session + 登入紀錄（不寄 email）
```

approval token 只保存 SHA-256 摘要、短時效且僅可消耗一次。email Yes 依然不是 GET 狀態變更，避免 email 預覽或連結掃描器直接登入。確認頁 session 僅透過同源 HttpOnly `Set-Cookie` 寫入，前端不可讀取或保存其值。

## 驗證

```text
PASS  UserAuthenticationServiceLoginSecurityTest：關閉通知的未知裝置不寄信，仍建立 device 與 session
PASS  UserAuthenticationServiceLoginSecurityTest：沒有綁定裝置時不寄 email，直接建立第一個 device 與 session
PASS  UserAuthenticationServiceLoginSecurityTest：email Yes 在確認頁 metadata 建立新的 device 與 session
PASS  UserAuthenticationServiceLoginSecurityTest：移除綁定裝置會一併撤銷該 device 的 active sessions
PASS  P12T09SchemaVerificationTest：V001–V012 在乾淨 PostgreSQL schema 完整套用
PASS  web npm run typecheck
PASS  web npm run build
PASS  Docker Compose 重建 server/web，V012 已隨 application startup 套用，health 為 UP
PASS  未附 session 呼叫 `GET /api/v1/account/security` 回 401，個人安全資料不對匿名公開
```

## 明確未完成

- 裝置撤銷、裝置命名、所有裝置登出、MFA/passkey、IP reputation、地理位置、rate limit、SIEM 與異常登入模型。
- TLS ingress、HSTS、CSRF、secret manager、production security review 與 production launch。

## Rollback

application rollback 可停止使用新的個人中心 security endpoint，且 `V012` 不回滾、不刪除使用者設定、裝置或 session evidence。既有資料庫使用者會保留預設 `TRUE`，部署者不得用 rollback 將未知裝置默默降級為無通知登入。

## 風險與人工審核

`HUMAN_REVIEW_REQUIRED: yes`。此變更會使 email Yes 在確認頁瀏覽器建立 session，人工審核應確認：GET 沒有狀態變更、Yes token 一次性原子消耗、No 不建立 session、裝置與 session metadata 取自確認頁 browser、設定只允許本人 session owner 修改、device list 不洩漏認證 secret，以及關閉通知的產品風險已被接受。
