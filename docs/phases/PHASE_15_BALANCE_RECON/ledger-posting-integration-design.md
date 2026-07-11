# Phase 15 - Ledger Posting Integration Design

## 目的

這份文件只定義正式 ledger posting integration 進入 runtime 前的設計順序與限制。
它不是正式 posting runtime，也不是 DB write 文件。

## Integration order

```text
1. request identity / idempotency decision
2. prerequisite gate
3. ledger invariant check
4. append transaction boundary
5. append ledger_journals / ledger_entries
6. outbox append
7. audit append
8. reconciliation marker
```

## 核心限制

```text
requestId 不等於完整 idempotency guarantee
accepted posting plan 不等於 posted / committed / persisted
LedgerAppendOnlyJdbcAdapter 不得被直接從 API / controller 呼叫
所有正式 posting integration 都是 HUMAN_REVIEW_REQUIRED
```

## 禁止事項

```text
不接正式 posting runtime
不新增 LedgerPostingService
不新增 @Repository
不新增 @Transactional
不新增 DB client call
不更新 balance_projections
```

## HUMAN_REVIEW_REQUIRED

```text
任何正式 posting integration 都屬於 HUMAN_REVIEW_REQUIRED。
任何把 accepted plan 誤寫成已完成資料寫入的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
