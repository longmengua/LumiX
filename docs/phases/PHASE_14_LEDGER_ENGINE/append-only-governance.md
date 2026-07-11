# Phase 14 - Ledger Append-Only Governance

## 目的

這份文件只說明 ledger persistence contract 的 append-only 治理要求。
它不是 repository 實作，也不是 DB migration 文件。

## Append-only contract

```text
ledger_journals 與 ledger_entries 只能以 append-only 方式延伸
既有 journal / entry 不應被直接更新或刪除
任何修正都應留待後續的 reversal / adjustment governance
```

## 交易設計要求

```text
正式 append runtime 未來必須把 idempotency check / lock、journal header append、journal entries append、outbox append、audit append 放在同一個 transaction 內。
balance_projections 不是這個 transaction 的 source of truth。
```

## Request identity 與 idempotency 分工

```text
requestId 只用於 trace / correlation / audit linkage，不可被誤解成完整 idempotency 保證。
idempotency key 才是未來用來防止同一 business operation 重複執行的主要契約。
business_reference_type + business_reference_id 只識別業務來源，不可單獨取代 idempotency key。
任何正式 idempotency runtime 都屬於 HUMAN_REVIEW_REQUIRED。
```

## Phase 12 schema mapping

```text
ledger_journals
  - business_reference_type
  - business_reference_id
  - request_id
  - journal_note
  - posted_at

ledger_entries
  - ledger_journal_id
  - entry_sequence
  - account_id
  - asset_symbol
  - direction
  - amount
```

## 後續正式 enforcement

```text
application rule
permission control
database trigger
operational review
```

## Persistence adapter gate

```text
任何 persistence adapter 若開始把 mapping contract 寫入 ledger_journals / ledger_entries，
仍然只代表 append-only gate 的最小實作，不能被誤解為完整 posting runtime。
這類 adapter 不得接到 posting command boundary 的正式 runtime path。
所有相關變更都屬於 HUMAN_REVIEW_REQUIRED。
```

## HUMAN_REVIEW_REQUIRED

```text
任何把 ledger persistence contract 接到正式寫入流程的變更都屬於 HUMAN_REVIEW_REQUIRED。
任何會影響 append-only 性質、journal 可追溯性或 double-entry 追蹤性的變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把單一 transaction 設計誤接成 runtime implementation 的變更都屬於 HUMAN_REVIEW_REQUIRED。
```
