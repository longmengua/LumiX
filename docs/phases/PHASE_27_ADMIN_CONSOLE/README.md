# Phase 27 - 管理後台基礎

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

建立最小權限、可追蹤且預設唯讀的營運管理能力。管理介面不是任意資料修正工具；不得直接調整 balance、ledger、reservation 或繞過風控。

## 中階 task breakdown

1. Admin identity/RBAC：角色、scope、MFA/session、break-glass policy 與職責分離。
2. Read-only operational views：deposit/withdrawal、risk、market health、reconciliation、audit evidence；須保留 as-of/health 與資料來源。
3. Controlled actions：只定義已批准 command 的 request/review/dual-control boundary；每個敏感 action 分離 task card 與 rollback/reversal 語意。
4. Audit/security：不可否認的 actor、reason、before/after reference、approval chain、export access 與 alerting。
5. UI/operational test evidence：權限拒絕、session expiry、雙人覆核、不可逆 action、資料最小化與無 production mock claim。

## 風險門檻

`HUMAN_REVIEW_REQUIRED: yes`。未完成 P26 policy、P28 evidence 或安全評估時，不得啟用寫入型管理動作；manual balance adjustment 永遠不在本 phase charter 內。
