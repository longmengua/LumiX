# Phase 15 - Trading Runtime Core Safety Contracts

## 目的

這份文件只列出 Trading Runtime Core 的安全契約與禁止誤用點。

## Safety contracts

```text
所有 amount / price / quantity 必須 BigDecimal，不得 float / double
所有 money movement 必須 HUMAN_REVIEW_REQUIRED
requestId 不等於 idempotency guarantee
idempotency key 才能代表 duplicate prevention contract
ledger append 成功不代表 balance projection 已同步
balance projection 不可作為資金真相來源
reservation 不可直接改 ledger，必須經 application boundary
reservation hold 會降低 available_amount，release 會增加 available_amount
reservation commit 必須經 settlement / ledger posting gate
order intake 可以要求 reservation，但不得直接寫 reservation DB
matching 不得偷寫 reservation 或 balance_projections
reconciliation 必須比較 ledger_entries derived totals、balance_projections rows、reservation locked amounts、settlement expected movements
settlement 必須是 explicit process，不可由 matching 或 order runtime 偷寫 ledger
所有正式 DB write 必須有 rollback / reconciliation 測試
所有高風險 runtime 不得 auto-commit
所有 reservation runtime 都屬於 HUMAN_REVIEW_REQUIRED
所有 reconciliation runtime 都屬於 HUMAN_REVIEW_REQUIRED
```

## P15 內允許的設計工作

```text
ledger posting integration design
balance projection rebuild / read model design
reservation hold / release design
reconciliation design
reconciliation check design
```

## 不在 P15-T01

```text
正式過帳
正式 balance mutation
正式 reservation hold / release
正式 settlement
正式交易下單
```

## HUMAN_REVIEW_REQUIRED

```text
所有 money movement runtime 都屬於 HUMAN_REVIEW_REQUIRED。
任何把這份文件誤寫成 production-ready safety proof 的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
