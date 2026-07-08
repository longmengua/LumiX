# Phase 14 - Ledger Persistence Mapping Contract

## 目的

這份文件定義 ledger journal draft 到 Phase 12 schema 欄位的對應方式。
它只描述 mapping contract，不包含 repository 實作或任何資料庫寫入流程。

## Journal header mapping

```text
ledger_journals
  business_reference_type <- LedgerBusinessReferenceType
  business_reference_id   <- businessReferenceId
  request_id              <- requestId
  journal_note            <- journalNote
  posted_at               <- postedAt
```

## Entry mapping

```text
ledger_entries
  ledger_journal_id <- future journal row identifier resolved by persistence layer
  entry_sequence    <- entrySequence
  account_id        <- accountId
  asset_symbol      <- assetSymbol
  direction         <- direction
  amount            <- amount
```

## Mapping rule

```text
mapping contract only describes shape and field intent
it does not write data
it does not rewrite existing rows
it does not update balance_projections
it does not resolve business rules
```

## HUMAN_REVIEW_REQUIRED

```text
所有 ledger persistence 相關內容都屬於 HUMAN_REVIEW_REQUIRED。
如果未來把這份 mapping contract 接到正式 repository 或資料庫 client，必須先完成 review。
```
