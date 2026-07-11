# Phase 14 - Ledger Runtime Integration No-Go Gate

## 目的

這份文件只說明 Phase 14 為什麼仍然禁止把 ledger posting boundary 接到正式 runtime。
它不是正式 runtime 設計，不是 migration 文件，也不是 database write 方案。

## No-Go 條件

```text
LedgerPostingCommandBoundary 沒有接 LedgerAppendOnlyJdbcAdapter
append adapter 只是 persistence gate，不是正式 posting runtime
idempotency / outbox / audit 仍只是設計，不是 runtime
balance_projections 沒有被 ledger append adapter 更新
沒有 LedgerPostingService
沒有 @Repository / @Transactional
沒有正式 DB client 寫入接線
```

## 檢查範圍

```text
source-scan
architecture guardrail test
phase 14 文件
```

## HUMAN_REVIEW_REQUIRED

```text
任何把 no-go gate 解讀成可以開始 production ledger posting 的行為都屬於 HUMAN_REVIEW_REQUIRED。
任何把 no-go gate 擴張成 runtime implementation 的變更都屬於 HUMAN_REVIEW_REQUIRED。
```
