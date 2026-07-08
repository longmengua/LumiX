# Phase 14 - Immutable Ledger Engine Runtime Prerequisites

## 目的

這份文件只定義 ledger runtime 可以開始之前必須先存在的邊界與資料前提。
它不是 posting 設計，也不是 double-entry 實作文件。

## Scope gate

```text
ledger_journals / ledger_entries 是 Phase 12 schema foundation
ledger 是資金真相來源 source of truth
balance_projections 只是 read model
double-entry invariant 必須由 posting service / tests / reconciliation 保證，不是單靠 DB CHECK
append-only policy 後續要由 application rule、permission、trigger 或 operational control 強化
所有 ledger runtime 變更都屬於 HUMAN_REVIEW_REQUIRED
```

## Runtime prerequisites

```text
Identity boundary
  - 必須能穩定解析 user / principal 身分
  - 不可把匿名或未驗證身分直接送進 ledger runtime

Account boundary
  - 必須能驗證 account 是否存在、是否啟用、屬於哪種 account type
  - 不可把 account existence 當成可選條件

Asset boundary
  - 必須能驗證 asset symbol 與可用精度
  - 不可讓未定義資產直接進入 ledger runtime

Market boundary
  - 必須能判斷 trading symbol 與 market state
  - 不可把 market closed / halted / unavailable 的狀態包裝成成功

Data foundation
  - ledger_journals 與 ledger_entries 先由 Phase 12 schema 提供
  - balance_projections 僅能作為可重建 read model
```

## 不在本階段處理

```text
ledger posting runtime
balance mutation
reservation hold / release
settlement mutation
order matching
withdrawal signing / broadcast
deposit crediting
admin balance adjustment
```

## 維運提醒

```text
任何 ledger runtime 實作都必須經過 application boundary、測試與人工審核。
如果未來某個 change 直接影響資金真相或 append-only 性質，應預設為 HUMAN_REVIEW_REQUIRED。
```
