# Phase 32 - 災難復原與重放

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

以 RPO/RTO、immutable evidence 與 deterministic replay 為準則，建立 backup/restore/reconciliation/incident recovery 能力；replay 不得暗中修正資金或交易資料。

## 高層任務

1. 資產與資料分級、backup/restore、encryption、retention、restore isolation 與 RPO/RTO target。
2. Ledger、balance、reservation、market、deposit/withdrawal、risk/audit 的重放輸入與 deterministic verification。
3. Recovery orchestration：freeze、read-only mode、reconciliation、exception escalation、resume criteria 與 human approval。
4. Regional/provider failure、corrupt data、partial restore、key loss 的 tabletop 與實演。
5. Recovery evidence、post-incident review、runbook 修正與 launch gate handoff。

## Gate

`HUMAN_REVIEW_REQUIRED: yes`；未完成可驗證 restore、replay mismatch handling 與資金對帳演練前，不得通過 launch gate。
