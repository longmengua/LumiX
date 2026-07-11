# Phase 17 - Futures Core Model

## 狀態

```text
planned, not started
```

## 目標

建立 futures core model 的 sandbox 前置層，只做 isolated margin、position model、leverage config 與 margin check。

## Core model 內容

```text
futures account model
isolated margin only
position model
leverage config
margin check
```

## 不在 phase

```text
cross margin
liquidation runtime
funding runtime
formal futures trading
```

## 高層 task list

```text
T01 futures account model
T02 isolated margin position model
T03 leverage config model
T04 margin check gate
T05 no-formal-trading review
```

## Sandbox 限制

```text
這一階段只建立 futures core model，不能宣稱可正式交易。
futures / margin runtime 仍屬 HUMAN_REVIEW_REQUIRED。
```

## HUMAN_REVIEW_REQUIRED

```text
任何 futures / margin runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把 isolated margin 誤寫成完整 futures trading 的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
