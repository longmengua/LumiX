# Architecture Text Map

本文件只放整體地圖。細節請跳到 `docs/architecture/`、`docs/backend/`、`docs/exchange-core/`。

## System context

```text
+------------------+       +------------------+       +------------------+
| Retail user      | <---> | LumiX Web        | <---> | LumiX API        |
+------------------+       +------------------+       +------------------+
                                                              |
+------------------+       +------------------+               |
| Operator         | <---> | Admin Console    | <-------------+
+------------------+       +------------------+
                                                              |
                                                              v
                                                     +------------------+
                                                     | Exchange System  |
                                                     +------------------+
                                                              |
            +------------------+----------------------+------------------+
            |                  |                      |                  |
            v                  v                      v                  v
+------------------+ +------------------+ +------------------+ +------------------+
| PostgreSQL       | | Redis            | | Event Bus        | | Wallet / Chain   |
| source of truth  | | cache / locks    | | async workflow   | | integrations     |
+------------------+ +------------------+ +------------------+ +------------------+
```

## Container map

```text
+--------------------------------------------------------------------------------+
| LumiX                                                                         |
|                                                                                |
|  +-------------+       +----------------+       +---------------------------+   |
|  | Web App     | <---> | API Gateway    | <---> | Backend Modules          |   |
|  | React/TS    |       | Spring API     |       | account / order / wallet |   |
|  +-------------+       +----------------+       +---------------------------+   |
|                                                   |                            |
|                                                   v                            |
|                                         +-------------------+                  |
|                                         | Exchange Core     |                  |
|                                         | ledger/matching   |                  |
|                                         +-------------------+                  |
|                                                   |                            |
|  +-------------+       +----------------+       +---------------------------+   |
|  | PostgreSQL  | <---- | Outbox Worker  | ----> | Event Bus / Workers      |   |
|  +-------------+       +----------------+       +---------------------------+   |
|                                                                                |
+--------------------------------------------------------------------------------+
```

## Runtime flow index

```text
Deposit        -> docs/architecture/runtime-flows.md#deposit-flow
Order          -> docs/architecture/runtime-flows.md#spot-order-flow
Cancel         -> docs/architecture/runtime-flows.md#cancel-order-flow
Settlement     -> docs/exchange-core/settlement-contract.md
Withdrawal     -> docs/architecture/runtime-flows.md#withdrawal-flow
Reconciliation -> docs/exchange-core/reconciliation.md
```

## Domain boundary index

```text
Account / identity       -> docs/backend/module-map.md
Ledger invariants        -> docs/exchange-core/ledger-invariants.md
Reservation state        -> docs/exchange-core/reservation-state-machine.md
Order lifecycle          -> docs/exchange-core/order-lifecycle.md
Matching contract        -> docs/exchange-core/matching-engine-contract.md
Wallet boundary          -> docs/exchange-core/wallet-boundary.md
Risk control             -> docs/exchange-core/risk-control.md
Operations readiness     -> docs/operations/go-no-go-checklist.md
```
