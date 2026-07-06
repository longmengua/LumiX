# Production Readiness Gates

LumiX 只有在所有 gate 通過後，才能宣稱 production launch ready。

## Gate map

```text
+------------------+       +------------------+       +------------------+
| Data integrity   | ----> | Funds safety     | ----> | Trading safety   |
+------------------+       +------------------+       +------------------+
          |                         |                         |
          v                         v                         v
+------------------+       +------------------+       +------------------+
| Security         | ----> | Operations       | ----> | Business launch  |
+------------------+       +------------------+       +------------------+
```

## Data integrity gate

- Migration 可重跑且可驗證。
- 所有金額欄位 precision 明確。
- 所有交易唯一鍵與 idempotency key 明確。
- Outbox / audit log 存在。
- Ledger append-only schema 存在。

## Funds safety gate

- Ledger invariant 測試通過。
- Balance projection 可由 ledger 重建。
- Reservation hold/release/capture 可對帳。
- Deposit crediting 有 confirmation policy。
- Withdrawal 需要 approval / signing / broadcast / reconciliation。

## Trading safety gate

- Order lifecycle 狀態完整。
- Matching output deterministic。
- Settlement atomic 或具備補償流程。
- Fee rounding policy 明確。
- Market halt / risk limit 有管理流程。

## Security gate

- Authentication / authorization 通過。
- Admin action audit trail 完整。
- Secrets 不落庫、不進 log。
- Rate limit / abuse protection 存在。
- Withdrawal 權限分離。

## Operations gate

- Metrics、logs、traces、alerts 可用。
- Runbook 完整。
- Incident escalation path 完整。
- Backup / restore / replay 演練完成。
- On-call 與交接可執行。

## Business launch gate

- Fee schedule 可管理。
- Revenue ledger 可查詢。
- User terms / compliance / support 流程準備完成。
- Human reviewer 明確簽核。
