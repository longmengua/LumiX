# P12 Data Model

## Text ERD

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

## Core tables

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

## Money precision

All money columns must be decimal or integer-minor-unit with explicit precision. No float, real, or double precision for money.
