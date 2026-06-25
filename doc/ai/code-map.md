# AI Code Map Index

This file is only the directory for agent-facing code maps. Keep detailed maps split under `maps/` so the agent can read only the task-relevant area.

## Maps

| Area | File | Use When |
| --- | --- | --- |
| Order and matching | [maps/order-matching.md](maps/order-matching.md) | Order placement, amend/cancel, matching engine, order book snapshots, lifecycle events. |
| Risk, ledger, and funds | [maps/risk-ledger-funds.md](maps/risk-ledger-funds.md) | Pre-trade risk, margin, wallet ledger, funding, liquidation, reconciliation. |
| Reliability and market data | [maps/reliability-market-data.md](maps/reliability-market-data.md) | Outbox, DLQ, recovery, Kafka event store, depth/ticker/trade/kline, push streams. |
| Polymarket and security | [maps/polymarket-security.md](maps/polymarket-security.md) | Polymarket market/order/session flows, signing, user WebSocket, API auth, tracing. |
| Market-maker and hedging | [maps/market-maker-hedging.md](maps/market-maker-hedging.md) | Market-maker quoting, inventory, hedge interfaces, hedge strategy, and hedge audit trail. |
| Web applications | [maps/web-apps.md](maps/web-apps.md) | Client web, admin web, frontend scope, and backend API areas. |
| Persistence and tests | [maps/persistence-tests.md](maps/persistence-tests.md) | Flyway migrations, Redis/Kafka contracts, focused test selection. |

## Update Rule

When a core flow changes, update only the matching sub-map and this index if a new area is introduced.
Do not let any single sub-map become a full design document; link to product/technical docs for long explanations.
