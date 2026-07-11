# Phase 15 - Reservation Hold/Release Design

## 目的

這份文件只定義 reservation hold/release 的設計門檻。
它描述未來 reservation runtime 必須如何影響 available_amount / locked_amount，但本階段不實作正式 hold / release / commit / cancel runtime。

## Reservation lifecycle

```text
HOLD    -> 降低 available_amount，增加 locked_amount
RELEASE -> 增加 available_amount，降低 locked_amount
COMMIT  -> 代表 reservation 已被消耗，必須進入 settlement / ledger posting gate
CANCEL  -> 代表 reservation rollback / cancel，必須可追蹤、可審計、可重試
```

## Boundary rules

```text
reservation 只能透過 application boundary 建立 / 釋放 / commit / cancel
reservation hold / release 不等於 ledger transfer
commit 必須經 settlement / ledger posting gate
ledger 是 source of truth
balance_projections 是 read model
order intake 可以要求 reservation，但不得直接寫 reservation DB
matching 不得偷寫 reservation 或 balance_projections
requestId 不是 idempotency guarantee
idempotency key 才能防 duplicate hold / release
```

## HUMAN_REVIEW_REQUIRED

```text
所有 reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED。
任何把 reservation 誤寫成正式資金變動完成態的行為都屬於 HUMAN_REVIEW_REQUIRED。
任何把 hold / release 直接接到 controller、matching 或 balance projection runtime 的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
