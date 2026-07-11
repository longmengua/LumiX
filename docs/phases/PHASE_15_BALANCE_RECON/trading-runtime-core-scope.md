# Phase 15 - Trading Runtime Core Scope Gate

## 目的

這份文件只定義 Trading Runtime Core 的 scope gate，不代表正式 runtime 已經存在。

## Scope boundary

```text
ledger append 是 source of truth 的候選執行路徑，但尚未正式接線
balance_projections 是 read model，不是 source of truth
reservations 是 hold / release runtime 的獨立邊界，不得偷渡進 ledger adapter
settlement 必須經過 ledger invariant、idempotency、append-only、reconciliation gate
```

## 可做的最早工作

```text
ledger posting integration design
balance projection rebuild / read model design
reservation hold / release design
reconciliation check design
```

## 禁止事項

```text
真正過帳
真正更新餘額
真正 reservation hold / release
真正 settlement
真正交易下單
任何 production-ready 宣稱
```

## HUMAN_REVIEW_REQUIRED

```text
所有 money movement runtime 都屬於 HUMAN_REVIEW_REQUIRED。
所有 ledger / balance / reservation / settlement runtime 都屬於 HUMAN_REVIEW_REQUIRED。
```
