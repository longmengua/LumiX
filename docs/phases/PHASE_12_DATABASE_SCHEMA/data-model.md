# P12 Data Model

## 文字 ERD

```text
users 1---n accounts 1---n account_assets
assets 1---n account_assets
assets 1---n markets
markets 1---n orders
orders 1---n trades
orders 1---n reservations
ledger_journals 1---n ledger_entries
ledger_entries n---1 accounts
ledger_entries n---1 assets
withdrawals n---1 accounts
deposits n---1 accounts
outbox_events n---1 aggregate reference
audit_logs n---1 actor reference
```

## 核心資料表

```text
users
accounts
assets
markets
account_assets
ledger_journals
ledger_entries
balance_projections
reservations
orders
trades
settlements
deposit_addresses
deposits
withdrawals
chain_transactions
outbox_events
audit_logs
idempotency_keys
admin_actions
```

## Money 精度

All money columns must be decimal or integer-minor-unit with explicit 精度. No float, real, or double 精度 for money.
