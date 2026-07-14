# 正式上線就緒門檻

LumiX 只有在所有門檻通過後，才能宣稱正式上線就緒。

## 門檻圖

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

## 資料完整性門檻

- Migration 可重跑且可驗證。
- 所有金額欄位精度明確。
- 所有交易唯一鍵與 idempotency key 明確。
- Outbox / audit log 存在。
- 帳本 append-only schema 存在。

## 資金安全門檻

- 帳本不變式測試通過。
- 餘額投影可由 ledger 重建。
- 預留 hold / release / capture 可對帳。
- 入金入帳有確認政策。
- 提款需要 approval / signing / broadcast / reconciliation。

## 交易安全門檻

- 訂單 lifecycle 狀態完整。
- Matching output deterministic。
- 結算具備 atomicity 或補償流程。
- 手續費取整政策明確。
- 市場暫停 / 風控限制有管理流程。

## 安全門檻

- Authentication / authorization 通過。
- Admin 動作 audit trail 完整。
- Secrets 不落庫、不進 log。
- Rate limit / abuse protection 存在。
- 提款權限分離。

## 營運門檻

- Metrics、logs、traces、alerts 可用。
- Runbook 完整。
- Incident escalation path 完整。
- Backup / restore / replay 演練完成。
- On-call 與交接可執行。

## 商業上線門檻

- Fee schedule 可管理。
- Revenue ledger 可查詢。
- User terms / compliance / support 流程準備完成。
- 人工審核者 明確簽核。
