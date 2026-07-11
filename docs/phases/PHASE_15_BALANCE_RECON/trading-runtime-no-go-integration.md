# Phase 15 - Trading Runtime No-Go Integration

## 目的

這份文件只定義 Phase 15 的 no-go integration gate。
它明確禁止把 order / matching / settlement / futures / liquidation / withdrawal / security runtime 偷接進 trading runtime core。

## No-go boundaries

```text
不得新增正式 trading runtime
不得新增 reservation runtime
不得新增 settlement runtime
不得新增 order / matching runtime
不得新增 futures / liquidation runtime
不得新增 wallet withdrawal runtime
不得新增 idempotency runtime
不得新增 outbox runtime
不得新增 audit runtime
不得新增新的 DB write path
```

## HUMAN_REVIEW_REQUIRED

```text
任何把 Phase 15 誤接成正式交易 runtime 的行為都屬於 HUMAN_REVIEW_REQUIRED。
任何把 no-go gate 當成 runtime complete 的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
