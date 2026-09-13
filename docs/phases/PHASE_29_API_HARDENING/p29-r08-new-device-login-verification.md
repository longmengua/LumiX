# P29-R08：新裝置登入 email 驗證與裝置安全快照

## 任務目的

當帳密被用於新的瀏覽器／裝置，或既有裝置的 browser fingerprint 改變時，不能直接建立 server-side session。系統必須把本次登入的 IP、去敏裝置名稱與時間通知帳號 email，由使用者明確選擇 Yes 或 No。此文件的「原登入瀏覽器完成登入」舊行為已由 P29-R10 取代，現行行為請見 `p29-r10-login-notification-preference-email-browser-session.md`。

```text
原登入 browser                         Email 確認 browser
      |                                          |
POST /auth/login (帳密 + device cookie)          |
      |                                          |
      +--已知裝置 + fingerprint 相符--> session   |
      |                                          |
      +--未知／變更--> pending HttpOnly cookie    |
                         |                        |
                         +---- SMTP (IP/裝置/時間、Yes/No) --->
                                                          |
                                               GET 確認頁（不改狀態）
                                                          |
                                               POST Yes 或 No
                                                          |
      POST /complete + pending cookie + fingerprint      |
      |                                                   |
      +--APPROVED + 相符--> trusted device + session     |
      +--PENDING / REJECTED / 不相符--> 不建立 session   |
```

## 完成內容

- `V011` append-only migration：新增 `user_login_devices` 與 `login_verification_requests`，並在 `user_sessions` 補入成功登入時的 IP／裝置快照。原始 device、pending、email token 都只保存 SHA-256 摘要。
- 首次註冊建立第一個受信任裝置與 session；既有帳號的舊 session 沒有裝置快照，下一次重新登入會被視為新裝置。
- `POST /api/v1/auth/login`：可信 device cookie 與 server-side browser fingerprint 相符才回傳 session；其他成功帳密回 `202`，寫入短效 `LUMIX_LOGIN_VERIFICATION` HttpOnly cookie，且不回傳 request id、token 或使用者資料。
- `POST /api/v1/auth/login-verification/decision`：email 確認頁明確送出 Yes／No。email 連結本身只打開前端頁面，沒有 GET 狀態變更，避免掃描器或預覽器意外核准。
- `POST /api/v1/auth/login-verification/complete`：只有原登入 browser 的 pending cookie、候選 device secret 與 browser fingerprint 都和資料庫記錄相符，且請求已 APPROVED，才能原子建立 trusted device 與 session；REJECTED、過期、已消耗或任何不符都不建立 session。
- SMTP 信件為 HTML Yes／No 按鈕，內容含 IP、去敏裝置標籤與 UTC 時間；按鈕仍會導向確認頁進行最後一次 POST。
- 本人登入紀錄 API 擴充 `ipAddress`、`deviceLabel` 安全快照；不回傳 session ID、cookie、token、完整 User-Agent 或 password。前端帳戶頁只向同一個已認證使用者呈現這兩個欄位。

## Browser fingerprint 邊界

fingerprint 是 User-Agent、Client Hints 與 Accept-Language 的 SHA-256 摘要，避免保存完整 User-Agent，並讓通常情況下被複製的 pending cookie 不能直接在不同 browser 完成登入。

它不是不可偽造的硬體身分：高能力攻擊者仍可能模擬 browser headers。因此安全性不能只依賴 fingerprint；主要不變式仍是高熵 HttpOnly secret、短效 15 分鐘、資料庫摘要、`FOR UPDATE` 原子消耗與「email 決定不等於建立 session」的雙通道限制。

IP 來自 servlet request 的 `remoteAddr`。本機 Compose Nginx 會設定 `X-Forwarded-For`，Spring 的 forwarded header strategy 只可部署在 server 位於可信 reverse proxy 後方時使用；server 直接公開時不得信任 forwarded headers。

## 設定與 migration

- 沿用已啟用的 `LUMIX_AUTH_PASSWORD_RESET_SMTP_ENABLED`、SMTP STARTTLS、寄件地址與 `LUMIX_AUTH_PUBLIC_BASE_URL`，避免額外建立未受控的寄信設定。
- 新增 `LUMIX_AUTH_LOGIN_VERIFICATION_TTL`，預設 `PT15M`；`LUMIX_AUTH_DEVICE_TTL` 預設 `P90D`。
- `V011` 必須在 `V001`–`V010` 後由 Flyway append-only 套用。application rollback 時可停用新裝置登入 endpoint，但不得刪除 trusted device、verification request 或 session security evidence。只有不含使用者資料的受控開發資料庫才可由人類核准重建。

## 明確未完成

- 裝置清單、手動撤銷、所有裝置登出 UI、地理位置、IP reputation、異常登入模型、rate limit、MFA／passkey、SIEM 與 production security monitoring。
- TLS ingress、公開 deployment、secret manager 與 production launch。此 task 不代表 production-ready。

## 驗證項目

```text
PASS  known device + 相同 fingerprint -> 200 session
PASS  unknown device + 正確帳密 -> 202 pending cookie，沒有 session
PASS  email Yes -> 原始 browser complete -> 200 session + trusted device cookie
PASS  email No -> 原始 browser complete -> 401，沒有 session
PASS  pending cookie 在 fingerprint 不同 browser -> 401，沒有 session
PASS  email token 單獨只能改決定，不能建立 session
PASS  login-history 僅本人 session 可讀，並顯示 snapshot；沒有 session/token/User-Agent
```

## 風險與人工審核

`HUMAN_REVIEW_REQUIRED: yes`。這是 authentication／authorization runtime change。人工審核應特別確認：未知裝置不會在寄信失敗時降級登入、GET 不能核准、No 不能被覆寫、approved request 只能消耗一次、email token 不會建立 session、pending cookie 與 fingerprint 都被驗證、cookie 皆為 HttpOnly/SameSite/secure 設定相符，以及可信 proxy 邊界正確。
