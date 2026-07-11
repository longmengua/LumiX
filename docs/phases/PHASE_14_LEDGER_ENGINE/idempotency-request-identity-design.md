# Phase 14 - Ledger Idempotency and Request Identity Design

## 目的

這份文件只描述 ledger posting 相關的 request identity 與 idempotency contract。
它不代表已經有正式 idempotency runtime，也不代表已經可以查詢或寫入 `idempotency_keys`。

## 核心分工

```text
requestId
  -> 只用於 trace / correlation / audit linkage
  -> 不能單獨保證 duplicate prevention

idempotency key
  -> 只用於防止同一 business operation 重複執行
  -> 之後必須配合 scope / lock / status 判斷

business_reference_type + business_reference_id
  -> 只識別業務來源
  -> 不能單獨取代 idempotency key
```

## 期望決策狀態

```text
NEW_REQUEST
DUPLICATE_COMPLETED
IN_PROGRESS
CONFLICTING_REQUEST
EXPIRED_OR_RETRY_REQUIRED
```

## 預期順序

```text
command received
  -> inspect request identity
  -> evaluate idempotency contract
  -> verify runtime prerequisites
  -> validate journal invariant
  -> build append-only posting plan
```

## 範圍限制

```text
不查詢或寫入 idempotency_keys
不接 LedgerPostingCommandBoundary 到 DB adapter
不實作正式 idempotency runtime
不新增 @Transactional
不新增 @Repository
不改 migration
不更新 balance_projections
```

## HUMAN_REVIEW_REQUIRED

```text
任何正式 idempotency runtime、lock、lookup、retry 或 replay 行為都屬於 HUMAN_REVIEW_REQUIRED。
任何把 requestId 誤寫成完整 idempotency 保證的變更都屬於 HUMAN_REVIEW_REQUIRED。
```
