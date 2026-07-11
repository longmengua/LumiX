# Phase 16 - Spot Trading Sandbox

## 狀態

```text
planned, not started
```

## 目標

建立 spot trading sandbox，驗證 order lifecycle、minimal matching、spot settlement 與 sandbox E2E，不進入 production trading。

## Sandbox 內容

```text
spot order lifecycle
minimal matching engine
spot trade settlement
spot sandbox E2E
```

## 不在 phase

```text
futures
production trading
real money withdrawal
public trading
```

## 高層 task list

```text
T01 spot order lifecycle
T02 minimal matching engine
T03 spot trade settlement
T04 spot sandbox E2E gate
T05 sandbox no-production review
```

## Sandbox 限制

```text
這只是 sandbox only 路線，不是 production trading。
spot / matching / settlement runtime 仍屬 HUMAN_REVIEW_REQUIRED。
```

## HUMAN_REVIEW_REQUIRED

```text
任何 spot / matching / settlement runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把 sandbox E2E 誤寫成正式交易上線證明的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
