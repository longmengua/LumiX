# Phase 19 - Risk Sandbox

## 狀態

```text
planned, not started
```

## 目標

建立 risk sandbox，先做 liquidation simulation、funding mock、insurance fund placeholder 與 risk / reconciliation tests。

## Sandbox 內容

```text
liquidation simulation
funding mock
insurance fund placeholder
risk / reconciliation tests
```

## 不在 phase

```text
production liquidation
real funding engine
real insurance fund accounting
production risk controls
```

## 高層 task list

```text
T01 liquidation simulation
T02 funding mock
T03 insurance fund placeholder
T04 risk / reconciliation tests
T05 production no-claim review
```

## Sandbox 限制

```text
這只是 simulation only，不是 production liquidation。
risk / liquidation / reconciliation runtime 仍屬 HUMAN_REVIEW_REQUIRED。
```

## HUMAN_REVIEW_REQUIRED

```text
任何 risk / liquidation / reconciliation runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把 simulation 誤寫成 production liquidation 的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
