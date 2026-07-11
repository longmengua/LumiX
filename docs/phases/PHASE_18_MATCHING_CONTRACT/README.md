# Phase 18 - Futures Trading Sandbox

## 狀態

```text
planned, not started
```

## 目標

建立受限 futures trading sandbox。這是加速路線中最早可以做受限合約交易 sandbox 的 phase，但仍不是 production。

## Sandbox 內容

```text
futures order placement
matching reuse
position update
realized / unrealized PnL
mock mark price
mock funding
restricted contract trade sandbox
```

## 不在 phase

```text
single market public launch
real money
public users
production trading
```

## 高層 task list

```text
T01 futures order placement
T02 matching reuse
T03 position update
T04 realized / unrealized PnL
T05 mock mark price and funding
T06 restricted contract sandbox gate
```

## Sandbox 限制

```text
P18 最早只能做受限 futures sandbox，不能視為正式合約交易。
單一 market、mock price only、sandbox only、不可接真錢、不可開放 public users。
```

## HUMAN_REVIEW_REQUIRED

```text
任何 futures / PnL / mark price / funding runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把受限 contract sandbox 誤寫成正式合約交易上線的行為都屬於 HUMAN_REVIEW_REQUIRED。
```
