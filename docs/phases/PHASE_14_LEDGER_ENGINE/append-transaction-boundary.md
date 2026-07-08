# Phase 14 - Ledger Append Transaction Boundary Design

## 目的

這份文件只設計 ledger append runtime 未來應該如何包在單一 transaction 內。
它不代表已經有 repository 實作，也不代表已經能寫入資料庫。

## 單一 transaction 的預期步驟

```text
1. idempotency check / lock
2. journal header append
3. journal entries append
4. outbox append
5. audit append
6. commit
```

## 不在本階段處理

```text
idempotency runtime implementation
outbox publisher implementation
audit writer implementation
database client implementation
balance_projections direct mutation
```

## 交易設計注意事項

```text
ledger append transaction 必須是 append-only
既有 journal / entry 不得直接 update 或 delete
requestId 必須同時進入 idempotency / audit / trace 設計
交易隔離、locking、retry、deadlock handling 只做設計記錄，不在本階段實作
balance_projections 不是 source of truth，不能在這個 transaction 內被當成資金真相更新
```

## 後續正式 gate

```text
command boundary
  -> append transaction boundary design
  -> persistence contract review
  -> repository implementation review
  -> transaction/retry/reconciliation review
```

## HUMAN_REVIEW_REQUIRED

```text
所有正式 ledger append runtime 都必須標示 HUMAN_REVIEW_REQUIRED。
任何把這份 transaction design 接到實際 DB 寫入流程的變更都屬於 HUMAN_REVIEW_REQUIRED。
```
