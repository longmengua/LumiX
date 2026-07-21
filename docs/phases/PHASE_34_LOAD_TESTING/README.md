# Phase 34 - 負載、浸泡、混沌測試

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

以可重現 workload 與故障場景驗證容量、隔離、降級與資料完整性；壓測不可使用真實客戶資金、secret 或未隔離 production path。

## 高層任務

1. Workload/profile、容量 target、資料生成、environment isolation 與 success/failure metric。
2. API、market data、wallet、risk、audit、admin 的 load/soak scenario 與 backpressure/queue limits。
3. Failure injection：provider loss、DB/cache/broker fault、network partition、slow consumer、reorg/resync、restore conflict。
4. Data integrity/reconciliation、security/rate-limit、SLO/alert/runbook 驗證。
5. Bottleneck remediation evidence、capacity plan、retest 與 human performance review。

## Gate

未完成 soak、chaos、data-integrity 與 recovery evidence，不得把單次 benchmark 視為 launch readiness；高風險測試計畫需 `HUMAN_REVIEW_REQUIRED`。
