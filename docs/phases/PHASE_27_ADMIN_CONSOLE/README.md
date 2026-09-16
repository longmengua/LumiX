# Phase 27 - 管理後台基礎

## 狀態

```text
COMPLETED_FOR_ADMIN_CONTROL_BOUNDARY_FOUNDATION
```

## Phase charter

建立最小權限、可追蹤且預設唯讀的營運管理 boundary foundation。管理介面不是任意資料修正工具；不得直接調整 balance、ledger、reservation 或繞過風控。

## 中階 task breakdown

1. Admin identity/RBAC：`COMPLETED_FOR_CONTRACT`；角色、MFA/session 與職責分離 evidence。
2. Read-only operational views：`COMPLETED_FOR_CONTRACT`；as-of/health/source evidence。

使用者管理的第一個 runtime 只提供已啟用最高管理員的使用者搜尋與細節讀取。清單僅支援顯示名稱的大小寫無關前綴搜尋（`keyword%`，禁止 `%keyword%`）、註冊時間與最後成功登入時間的半開區間，以及 `(created_at, user_id)` keyset pagination；回應同時回傳不套用 cursor 的精確 `total` 與 `pageSize`，僅供顯示總筆數／總頁數，翻頁位置仍必須使用 cursor。最後登入由每個使用者的最新 session 決定，讀取沿用既有 `(user_id, created_at DESC)` 索引。資料庫另有名稱 prefix 與游標索引，避免以 offset 或多欄位 OR 模糊搜尋造成全表掃描。部署可用 `LUMIX_ADMIN_SUPER_ADMIN_EMAIL` 指定首次啟用信收件人；啟用連結只使用受控 SMTP 傳遞，密碼與 token 皆只保存不可逆雜湊。無有效 server session 或尚未啟用的 principal 一律 403，且不提供任何停權、角色、MFA、資產或帳本寫入操作。其他後台模組仍保留 mock，待各自完成真實功能後才移除。

2026-09-15 ASSET-T09 增加 `/api/admin/v1/users/{userId}/assets`、`/accounts` 與 `/history` readonly view，仍以同一 server-side super-admin gate 授權。`/accounts` 只讀帳戶容器，與只讀既有 `balance_projections` 的 `/assets` 分開，讓空帳戶不會被誤顯示為零餘額；`/history` 最多讀取 20 筆該使用者帳戶的 immutable ledger entry。無資料時回傳空集合，沒有空投、manual adjustment、ledger append、開戶或 projection mutation。
3. Controlled actions：`COMPLETED_FOR_CONTRACT`；request/review dual-control evidence，無 command。
4. Audit/security：`COMPLETED_FOR_CONTRACT`；actor/reason/evidence-reference/export request contract。
5. UI/operational test evidence：`COMPLETED_FOR_FOUNDATION`；權限拒絕、session expiry、雙人覆核與 source health 測試。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。未完成 P26 policy、P28 evidence 或安全評估時，不得啟用寫入型管理動作；manual balance adjustment 永遠不在本 phase charter 內。
