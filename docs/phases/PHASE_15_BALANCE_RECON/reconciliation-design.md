# Phase 15 - Reconciliation Design

## 目的

這份文件只定義 reconciliation 的設計門檻。
它說明未來如何比較 ledger、balance_projections、reservation 與 settlement 的結果，但本階段不實作正式 reconciliation runtime。

## Reconciliation scope

```text
ledger 是 source of truth
balance_projections 是 read model，必須能從 ledger rebuild / replay
reservation 是 hold/release 狀態模型，不是 ledger entry 替代品
settlement 是 explicit process，不能由 matching / order runtime 偷寫 ledger
reconciliation 必須比較：
  - ledger_entries derived totals
  - balance_projections rows
  - reservation locked amounts
  - settlement expected movements
mismatch 必須產生 review / incident / repair flow，不得自動靜默修正
reconciliation 必須可追蹤 requestId，但 requestId 不是 idempotency guarantee
未來 reconciliation runtime 必須有 audit / outbox / idempotency 邊界
```

## HUMAN_REVIEW_REQUIRED

```text
任何 reconciliation runtime 都屬於 HUMAN_REVIEW_REQUIRED。
任何把 mismatch 自動靜默修正的行為都屬於 HUMAN_REVIEW_REQUIRED。
任何把 reconciliation 誤寫成已經完成的 production control loop 都屬於 HUMAN_REVIEW_REQUIRED。
```
