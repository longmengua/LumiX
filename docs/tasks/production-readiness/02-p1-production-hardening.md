# P1 Fine Tasks: Production Hardening

Status: `tracking`

## Goal

Split remaining P1 hardening items into small implementation slices for market data, Polymarket, database/storage, observability, and alerting.

## Fine-Grained Progress

`14/22` done.

## Market Data Gateway

- [x] Add gateway heartbeat contract for SSE/WebSocket clients.
- [x] Add subscription authorization check baseline for private user streams.
- [x] Add per-client market-data rate limiter.
- [x] Add disconnect recovery cursor contract for depth/trade streams.
- [x] Add deployment notes for horizontally scaled market-data gateway instances.

## Market Maker API Hardening

- [x] Add market-maker quote API frequency limit policy.
- [x] Add market-maker hedge execution API frequency limit policy.
- [x] Add market-maker endpoint audit fields for operator identity and approval token outcome.

## Polymarket Integration

- [x] Add local/CLOB/trade/settlement state machine transition matrix.
- [x] Persist Polymarket trade events into local order lifecycle projection.
- [x] Add settlement state transition and terminal-state downgrade protection tests.
- [x] Add Gamma response schema version wrapper for market discovery.
- [x] Add CLOB response schema version wrapper for order operations.
- [ ] Add user WebSocket checkpoint persistence and replay test.

## Database And Storage

- [ ] Add live order SQL mirror/index design note or implementation decision.
- [ ] Add live position SQL mirror/index design note or implementation decision.
- [ ] Add archive exporter job skeleton for historical orders/trades/ledger.

## Observability And Alerts

- [ ] Add metrics collectors for DB latency and Redis latency.
- [x] Add metrics collectors for matching latency, rejection rate, and fill rate.
- [ ] Add Kafka lag metric collector.
- [ ] Add tracing export configuration and sampling policy doc.
- [ ] Add alert rules for matching halt, DLQ buildup, reconciliation failure, and unbalanced assets.

## Read First

- [../../en/todo.md](../../en/todo.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/polymarket-security.md](../../ai/maps/polymarket-security.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
