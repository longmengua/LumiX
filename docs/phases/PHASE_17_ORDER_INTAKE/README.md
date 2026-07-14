# Phase 17 - Futures Core Model

## 狀態

```text
in progress
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
T01 futures account model - completed
T02 isolated margin position model - pending
T03 leverage config model - pending
T04 margin check gate - pending
T05 no-formal-trading review - pending
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

## T01 implementation notes

- Scope: 只建立 `com.lumix.trading.core.futures.account` 的 sandbox immutable domain model，不接 position、leverage、margin calculation、matching、settlement 或 ledger mutation。
- Model decisions: `FuturesAccount` 重用既有 `AccountId`、`UserId`、`AccountStatus`、`AssetSymbol`，並以 `FuturesMarginMode.ISOLATED` 固定目前唯一可用的 margin mode。
- Invariants: 必填欄位不可為 null，`updatedAt` 不可早於 `createdAt`，`marginMode` 不可離開 isolated。
- Verification: 已新增單元測試覆蓋 legal creation path、margin mode 封鎖、timestamp chronology 與 null input guard。
- Known limits: 這只是 sandbox domain model，沒有 position、leverage、margin check、liquidation、funding、futures order execution，也沒有任何 production trading API。
- HUMAN_REVIEW_REQUIRED: 所有 futures / margin 相關變更仍保留人工審核前提，Phase 17 不代表正式 futures runtime 已啟用。
