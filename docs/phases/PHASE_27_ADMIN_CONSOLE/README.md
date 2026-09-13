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

使用者管理的第一個 runtime 只提供已啟用最高管理員的使用者搜尋與細節讀取。部署可用 `LUMIX_ADMIN_SUPER_ADMIN_EMAIL` 指定首次啟用信收件人；啟用連結只使用受控 SMTP 傳遞，密碼與 token 皆只保存不可逆雜湊。無有效 server session 或尚未啟用的 principal 一律 403，且不提供任何停權、角色、MFA、資產或帳本寫入操作。其他後台模組仍保留 mock，待各自完成真實功能後才移除。
3. Controlled actions：`COMPLETED_FOR_CONTRACT`；request/review dual-control evidence，無 command。
4. Audit/security：`COMPLETED_FOR_CONTRACT`；actor/reason/evidence-reference/export request contract。
5. UI/operational test evidence：`COMPLETED_FOR_FOUNDATION`；權限拒絕、session expiry、雙人覆核與 source health 測試。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。未完成 P26 policy、P28 evidence 或安全評估時，不得啟用寫入型管理動作；manual balance adjustment 永遠不在本 phase charter 內。
