# P29-R06 — 已認證個人基本資料 API

## 狀態

```text
COMPLETED_FOR_AUTHENTICATED_PROFILE_FOUNDATION
HUMAN_REVIEW_REQUIRED: yes
```

## 目標

將個人中心總覽的基本身份資料從既有 mock 改為目前登入者自己的 PostgreSQL 資料，並提供受限的顯示名稱更新；讓使用者可維護非安全識別用途的名稱，而不擴張到 email、KYC、資產或任何資金流程。

## 已納入範圍

- `GET /api/v1/account/profile`：只回傳由 session principal 識別的 `userId`、`email`、`displayName` 與 `createdAt`。
- `PATCH /api/v1/account/profile`：只接受 `displayName`，重用 server 的 2–128 字元驗證規則；request 不接受 `userId` 或 `email`。
- PostgreSQL 更新以 `user_id` 與 `ACTIVE` 狀態為條件；在 session 驗證後帳號被停用時，更新必須失敗。
- 前端 `/account` 總覽讀取真實 profile、更新 header 的使用者投影，並保留改密碼與登入紀錄入口。
- 總覽明確告知 KYC、資產、劃轉、API Key 與偏好設定尚是開發期 mock，避免假資料被誤當成使用者真實資料。

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
GET / PATCH /api/v1/account/profile
        |
        v
users（僅本人、僅 display_name 可更新）
```

`userId` 永遠由 server-side session 決定，不能由 path、query 或 request body 覆寫。email 是登入識別資料，這個 task 不提供變更功能；email 變更必須另有驗證、通知、風控與 security review。profile response 不包含 password、session、token、KYC、權限、資產或任何帳本資料。

## 明確不含範圍

- email／手機變更、email verification、MFA、裝置與 session 管理、IP／地理位置或異常登入偵測。
- KYC、資產、餘額、劃轉、入金、提款、訂單、交易、API Key、通知與偏好設定 runtime。
- 新 schema 或 migration；本 task 只讀寫既有 `users.display_name`，不改動認證 credential 或任何資金資料。
- production launch 或 production-ready 宣稱。

## 驗證

```text
2026-09-10
PASS  web npm run typecheck
PASS  web npm run build
PASS  Docker server image build（含 Java 編譯）
PASS  Docker Compose 重建 server/web 並健康啟動
PASS  register -> GET /api/v1/account/profile：僅回傳本人基本 profile 與 createdAt
PASS  PATCH /api/v1/account/profile -> GET：displayName 已持久化且 GET/PATCH 使用同一 Cookie
PASS  未帶 Cookie -> GET /api/v1/account/profile：401
PASS  git diff --check
BLOCKED  本機 ./mvnw test：使用者 Maven settings 指向的 Nexus 連線逾時；此為環境相依下載失敗，不是 assertion 失敗
```

## Rollback

不需要 migration rollback。若 application rollback，停用這兩個 route 即可；既有 `users.display_name` 的更新是使用者可見資料，不能以刪除或回寫資料庫方式回滾。使用相容版本讀取既有欄位即可。

## 人工審核重點

`HUMAN_REVIEW_REQUIRED: yes`，因為新增 authenticated private API 與帳戶資料寫入。請檢查：owner 是否只來自 request principal、request 是否無法指定 userId/email、停用帳號是否拒絕更新、response 是否沒有 credential/session/KYC/資產資料，以及 email 變更是否仍被明確排除。
