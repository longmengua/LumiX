# Phase 15 - Trading Runtime Core

## 狀態

```text
planned, not started
```

## 目標

建立 accelerated trading sandbox track 的 runtime core gate，先把 ledger posting、balance projection、reservation 與基本 reconciliation 的 runtime 邊界收斂。

## Accelerated track 內容

```text
ledger posting runtime integration gate
balance projection runtime
reservation hold / release
basic reconciliation
```

## 不在本 phase

```text
matching
futures position
liquidation
real withdrawal
production trading
```

## 高層 task list

```text
T01 ledger posting runtime integration gate
T02 balance projection runtime
T03 reservation hold / release boundary
T04 basic reconciliation gate
T05 no production claim review
```

## Sandbox 限制

```text
這只是 accelerated track 的第一段，不是 production-ready。
ledger / balance / reservation runtime 仍屬 HUMAN_REVIEW_REQUIRED。
```

## HUMAN_REVIEW_REQUIRED

```text
任何 ledger / balance / reservation runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把這個 phase 誤解成正式交易上線門檻的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
