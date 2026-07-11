# Phase 14 - Ledger Append Persistence Adapter Gate

## 目的

這份文件只說明最小 ledger append persistence adapter 的邊界。
它不是 posting service，也不是完整 transaction orchestration。

## 範圍

```text
只接受已完成 invariant 檢查的 ledger append mapping contract
只 append ledger_journals 與 ledger_entries
不更新 balance_projections
不處理 idempotency runtime
不處理 outbox / audit runtime
不接 posting command boundary 的正式 runtime path
```

## 寫入語意

```text
adapter 可以在測試中直接驗證 append-only DB write
adapter 不得自行做 business decision
adapter 不得回頭修改既有 journal / entry
adapter 不得被誤解成完整 posting runtime
```

## HUMAN_REVIEW_REQUIRED

```text
任何把這個 adapter 接進正式 posting runtime 的變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把 append-only gate 誤升級為完整帳本引擎的變更都屬於 HUMAN_REVIEW_REQUIRED。
```
