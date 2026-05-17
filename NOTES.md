<!-- File purpose: English technical notes for java21-match-hub. Chinese version: NOTES_ch.md. -->
# Technical Notes

This file is the English companion to `NOTES_ch.md`. The Chinese file contains the longer original review notes. This English version summarizes the main engineering points so the original filename remains available for English readers.

## Current Direction

The project is organized around DDD-style layers:

- `interfaces`: REST APIs, DTOs, validators, exception handling, Kafka consumers, and push configuration.
- `application`: commands, use cases, application services, scheduled jobs, and event publishing contracts.
- `domain`: entities, DTOs, enums, repository contracts, domain services, domain events, and utility code.
- `infra`: concrete infrastructure adapters for Kafka, Redis, matching, HTTP clients, Web3j, and Spring configuration.

The codebase currently supports an internal exchange MVP and a Polymarket integration path. The internal exchange includes order entry, matching, margin/accounting, market data, risk settlement, recovery, Kafka events, Redis repositories, and tests around matching/accounting/risk behavior. The Polymarket path includes market discovery/sync, price refresh, CLOB credential handling, session signer flow, order signing, CLOB request signing, user WebSocket events, and approval checks.

## Strengths

- The layer boundaries are clear enough to support future service extraction.
- The core business concepts are visible in the model: order, account, position, wallet ledger, outbox, DLQ, prediction market, session, and Polymarket order.
- The in-memory matching engine is useful for MVP validation and unit tests.
- Kafka and Redis adapters are already represented behind application/domain contracts.
- The Polymarket integration avoids depending on an SDK for the main signing and request flows.

## Main Risks

- The matching engine is still in-memory and not yet replayable after process failure.
- Accounting consistency is not production-grade until order, ledger, position, event store, and outbox writes have explicit transaction and reconciliation rules.
- Risk controls need a real mark/index price source and a complete liquidation workflow.
- Secrets are still represented in configuration fields and must be injected through a secret manager or environment variables in production.
- Market-data and WebSocket delivery need sequence, checksum, replay, authorization, and horizontal-scaling design.

## Production Priorities

See `todo.md` for the detailed production-readiness checklist. The short version is:

1. Add durable order lifecycle events and production ledger schema.
2. Make matching replayable with command log, event log, snapshots, and checkpointing.
3. Add mark/index price, risk tiers, stronger pre-trade checks, and liquidation automation.
4. Add reconciliation jobs, metrics, tracing, structured logs, and alerts.
5. Split long-running workers such as matching, market data, and Polymarket user WebSocket from the REST app when load grows.
