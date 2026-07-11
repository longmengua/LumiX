# Phase 15 - Balance Projection Runtime Design

## 目的

這份文件只定義 balance projection 如何從 ledger source of truth rebuild / project 的設計門檻。
它不代表已經有任何 balance update runtime。

## Core statements

```text
ledger 是 source of truth
balance_projections 是 read model
projection 可以 rebuild / replay / reconcile
projection lag 必須可觀測
projection mismatch 必須進 reconciliation flow
projection runtime 未來必須能從 ledger_entries 推導 total / available / locked，但本次不實作
```

## 禁止事項

```text
不允許直接 insert / update / delete balance_projections runtime SQL
不把 balance_projections 當作資金真相來源
不讓 order / matching / settlement 直接把 balance_projections 當成 source of truth
不新增 BalanceProjectionService
不新增 BalanceMutationService
不新增 @Repository
不新增 @Transactional
```

## HUMAN_REVIEW_REQUIRED

```text
任何 balance projection runtime 都屬於 HUMAN_REVIEW_REQUIRED。
任何把 projection 設計誤寫成即時餘額真相的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
