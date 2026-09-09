# P29-R02 — 持久化使用者認證 Runtime

## 狀態

```text
COMPLETED_FOR_PERSISTENT_AUTH_RUNTIME_FOUNDATION
HUMAN_REVIEW_REQUIRED: yes
```

## 目標

讓前後端能使用 PostgreSQL 中的實際帳密與伺服器端 session 完成基本帳號流程，而不是繼續依賴前端 mock：註冊、登入、登出、目前使用者、改密碼，以及有受控 SMTP 時的忘記／重設密碼。

## 已納入範圍

- `V009` append-only migration：`user_credentials`、`user_sessions`、`password_reset_requests`。
- BCrypt 密碼雜湊；資料庫、log 與 API response 不保存或輸出明文密碼。
- 密碼最少 8、最多 32 個字元，且最多 72 UTF-8 bytes；上限仍符合 BCrypt 不可靜默截斷的限制。
- 高熵 session/reset secret；資料庫只保存 SHA-256 摘要。
- `/api/v1/auth/*` 同源 API 與 HttpOnly、SameSite=Strict Cookie。
- 註冊、登入與忘記密碼頁面採單一認證外框；桌面維持左側視覺、右側表單，右欄以頁面指定的 flex 比例分配上方留白與表單區：註冊 `1:5`、登入 `1:3`、忘記密碼 `1:2`。平板與手機改為上圖下表單並使用自然內容高度；完整 LumiX 品牌列為返回首頁控制項，避免重複的文字導航。
- Auth RWD 採 desktop（至少 1024px）完整視覺、tablet（768–1023px）縮小插圖與 mobile（小於 768px）compact 品牌標頭。手機不鎖定頁面高度或捲動，隱藏純裝飾並保留完整鍵盤可操作的表單與 48px touch target。
- 密碼變更與重設後撤銷所有既有 session。
- PostgreSQL primary transaction 驗證 session／密碼，避免 replica lag 讓撤銷狀態失真。
- 可選 SMTP delivery adapter；未配置時 forgot-password endpoint fail-closed，絕不以 log 或 response 回傳 reset token。

## 明確不含範圍

- email verification、MFA、KYC/AML、admin RBAC、API key、OAuth/SSO、rate limit、帳號風控與客服流程。
- SMTP provider / credential / secret manager / TLS ingress 的實際部署。
- 資金、帳本、交易、入金、提款、matching 或 settlement runtime。
- production launch 或 production-ready 宣稱。

## 密碼策略同步契約

```text
產品規則：最少 8、最多 32 個字元，且不得超過 72 UTF-8 bytes
server 裁決：UserAuthenticationService.validatePassword
前端預先檢查：features/auth/passwordPolicy.ts
適用入口：註冊、已登入改密碼、忘記密碼重設
登入相容：login 只保留 BCrypt 72 bytes 防護，不以新上限阻止既有帳號登入
```

前端 policy 只提供立即錯誤訊息，不能取代 server。任何長度變更都必須同時更新上述兩處、此文件與邊界測試；UI 不主動展示密碼規格，只在驗證失敗時顯示錯誤。

## Migration 與 rollback

`V009` 必須在既有 `V001`–`V008` 後由 Flyway append-only 套用。它不應以 production down migration 回滾：若 application rollback，先停止 auth route、保留認證稽核資料並以相容 application 版本讀取；只有未承載使用者資料的受控環境才可在人工核准後清除 schema/volume。不得透過刪除 session 或 credential 資料掩蓋安全事件。

## 已完成驗證

```text
2026-09-09
PASS  docker compose config --quiet
PASS  server image build 與 Flyway V009，schema 共 9 migrations
PASS  /actuator/health -> UP（SMTP 未啟用時 mail health 不誤報）
PASS  web npm run typecheck 與 web image build
PASS  同源 web proxy：register 201、me 200、logout 204、post-logout me 401
PASS  完整流程：register 201、me 200、change password 200、舊密碼 login 401、新密碼 login 200、logout 204、post-logout me 401
PASS  SMTP 未設定：forgot password 503 fail-closed，無 token 回傳
PASS  資料庫：credential 為 BCrypt；認證 schema 無 password 明文欄位
PASS  密碼長度 policy：8 字元註冊 -> 201；33 字元註冊 -> 400
PASS  auth RWD：320×568、360×800、375×667、390×844、430×932、768×1024、1024×768、1440×900 無 horizontal overflow；mobile 隱藏 decorative art 且首個 input 位於首屏。
PASS  mobile short viewport：390×420 時頁面保持可捲動（`overflow-y: visible`），登入 CTA 位於可視區內。
```

## 尚待驗證／阻擋條件

- SMTP 啟用需由人類指定受控提供者、`SPRING_MAIL_*` secret injection、STARTTLS、寄件地址與 HTTPS `LUMIX_AUTH_PUBLIC_BASE_URL`；目前預設為 fail-closed。
- 公網上線前需補齊 TLS ingress、CSRF/CORS policy review、rate/abuse protection、audit/monitoring、password policy/MFA、email verification 與 security review evidence。
