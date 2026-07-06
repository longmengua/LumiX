# LumiX Overview

LumiX 是一個以正式營運為目標的線上交易所系統。

## Product scope

```text
Retail user
  - register / login / account security
  - deposit assets
  - trade spot markets
  - view balances, orders, trades, fees
  - withdraw assets

Operator
  - monitor markets and wallets
  - review abnormal activity
  - handle listing, fee, risk, incident workflow
  - reconcile balances and chain transactions

System
  - keep immutable ledger
  - reserve funds before order placement
  - match orders deterministically
  - settle trades atomically
  - protect withdrawals through approval and signing boundaries
```

## Production stance

LumiX 的核心設計假設是真實資金會進入系統，因此所有金流資料必須：

- 可審計。
- 可追蹤。
- 可重放。
- 可對帳。
- 可事故復原。
- 可被人類審核。
