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
3. Controlled actions：`COMPLETED_FOR_CONTRACT`；request/review dual-control evidence，無 command。
4. Audit/security：`COMPLETED_FOR_CONTRACT`；actor/reason/evidence-reference/export request contract。
5. UI/operational test evidence：`COMPLETED_FOR_FOUNDATION`；權限拒絕、session expiry、雙人覆核與 source health 測試。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。未完成 P26 policy、P28 evidence 或安全評估時，不得啟用寫入型管理動作；manual balance adjustment 永遠不在本 phase charter 內。
