# P29-R04 — 已認證登入紀錄 API

## 狀態

```text
COMPLETED_FOR_AUTHENTICATED_READ_FOUNDATION
HUMAN_REVIEW_REQUIRED: yes
```

## 目標

將帳戶頁的「登入紀錄」從前端 mock 資料改為目前登入使用者自己的伺服器端成功登入 session 歷程，讓基本帳號流程能確認登入事件已被持久化並可回查。

## 已納入範圍

- `V010` append-only migration，為 `user_sessions (user_id, created_at DESC)` 增加 bounded history 查詢索引。
- `GET /api/v1/account/login-history`：由 P29-R03 全域 authentication filter 驗證 Cookie session 後，僅讀取 request principal 的資料。
- 固定最多 50 筆、依成功 session 建立時間倒序回傳；不接受 userId、limit 或其他可跨帳號查詢的輸入。
- response 僅包含 `occurredAt`；不回傳 session ID、secret digest、Cookie、IP、user agent 或密碼資料。
- 前端 `/account/login-history` 改由同源 HttpOnly Cookie API 讀取；契約錯誤或 API 失敗時不回退顯示舊 mock 登入紀錄。

## 明確不含範圍

- IP／裝置辨識、地理位置、異常登入偵測、MFA、通知、session 管理與撤銷 UI。
- 完整 security audit event、SIEM、保留政策、資料主體權利或隱私治理；這些需要獨立安全與合規設計。
- 帳戶總覽、資產、訂單、KYC 等其餘帳戶頁資料仍是既有 mock foundation，不能視為後端整合完成。
- 資金、帳本、交易、入金、提款或 production launch。

## 資料與安全邊界

```text
Browser HttpOnly Cookie
        |
        v
P29-R03 authentication filter
        |
        v
AuthenticatedUser request principal
        |
        v
GET /api/v1/account/login-history
        |
        v
user_sessions (own user_id, newest 50)
        |
        v
occurredAt only -> Account login-history view
```

session 建立時間是成功登入的既有權威資料。讀取使用 primary transaction，以避免剛成功登入後因 read replica 延遲而暫時看不到紀錄。此 API 不是 session 驗證本身，authentication filter 仍在 controller 前做唯一安全判斷。

## Migration 與 rollback

`V010` 必須在 `V001`–`V009` 後由 Flyway append-only 套用。它只新增索引，不改寫或刪除任一 session。application rollback 時可停止此 read endpoint，保留索引與 `user_sessions` 歷程；不得為了 rollback 而刪除 session 或認證資料。只有未含使用者資料的受控環境，才可在人工核准後重建 schema/volume。

## 已完成驗證

```text
2026-09-09
PASS  Docker server image build（含 Java 編譯）
PASS  web npm run typecheck
PASS  git diff --check
PASS  Docker Compose 啟動並由 Flyway 套用 V010
PASS  register -> GET /api/v1/account/login-history：回傳 1 筆 occurredAt
PASS  logout -> login -> GET /api/v1/account/login-history：回傳最新 2 筆
PASS  未帶 Cookie -> GET /api/v1/account/login-history：401
BLOCKED  本機 ./mvnw test：使用者 Maven settings 指向的 Nexus 連線逾時；提升權限後仍無法連線，非測試 assertion 失敗
```

## 人工審核重點

`HUMAN_REVIEW_REQUIRED: yes`，因為新增了 authenticated private API 與使用者安全歷程的讀取路徑。請檢查：只能依 request principal 查詢、response 沒有認證材料、history 上限固定、primary consistency 的取捨，以及未蒐集 IP／裝置資料是否符合後續隱私與資安設計。
