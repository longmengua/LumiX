# Phase 31 - 可觀測性與告警

## 狀態

```text
PLANNING_PROGRAM_DRAFTED_AWAITING_HUMAN_APPROVAL
```

## Phase charter

建立能揭露資料完整性、資金安全、交易安全與營運健康的 metrics/logs/traces/alerts；觀測資料不應含 secret 或未遮罩個資。

## 高層任務

1. Signal taxonomy、correlation ID、structured logging、trace propagation 與資料保留/遮罩。
2. Domain SLI/SLO：market stale/gap、deposit finality、withdrawal approval/sign/broadcast、ledger/reconciliation、risk rejection、API error/latency。
3. Alert routing、severity、deduplication、runbook、on-call escalation 與人工 acknowledgement。
4. Dashboard/evidence、audit access、missing telemetry detection 與 failure-mode tests。
5. Operational review：告警演練、false-positive/false-negative review、capacity signal handoff。

## Gate

未有完整敏感流程 signal、runbook 與 alert drill，不得進入 launch claim；涉及安全/資金監控設定為 `HUMAN_REVIEW_REQUIRED`。
