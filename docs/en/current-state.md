<!-- File purpose: quick snapshot of codebase completion, MVP capabilities, and production blockers. Chinese version: ../zh-TW/current-state.md. -->
# Current State

This document answers one question: how complete is this repository right now?

Short answer: this is a runnable and testable trading-core MVP, not a production-ready exchange. The core baselines exist, but real funds and real traffic still require durable state, replay, reconciliation, monitoring, and operational controls.

中文版本：[../zh-TW/current-state.md](../zh-TW/current-state.md)

## Completion Snapshot

The counts below come from the `[x]` / `[ ]` status in [todo.md](todo.md).

| Scope | Completed Baseline | Open Production Work | Reading |
| --- | ---: | ---: | --- |
| P0 Required | 25 | 11 | Core MVP capability exists, but many production blockers remain. |
| P1 Strongly Recommended | 5 | 17 | Operations, market data, Polymarket, and data governance are still early. |
| P2 Evolution | 0 | 5 | Admin, reporting, load testing, compliance, and rollout controls have not started. |
| Total | 30 | 33 | The project has a baseline, but production hardening is still the main body of work. |

## Capabilities You Can Reasonably Rely On

- Local MySQL, Redis, Kafka, and Kafka UI can be started with Docker Compose.
- Internal exchange order entry has an MVP chain: validation, pre-trade risk, in-memory matching, accounting updates, and event publishing.
- Matching behavior has deterministic tests for FIFO, post-only, self-match prevention, IOC/FOK, and insufficient market-order liquidity.
- An in-process per-symbol sequencer baseline serializes same-symbol matching operations within one process.
- A wallet-ledger balanced posting baseline makes MVP fund movements traceable and testable.
- Accounting entries are split for order reserve, position margin, fee, rebate, realized PnL, funding, liquidation shortfall, deposit, and withdrawal.
- Deposit and withdrawal have a state-machine baseline covering pending, confirmed, failed, reversed, and manual review.
- Account risk snapshot, persisted risk snapshot, pre-trade risk checks, risk tiers, global risk switches, mark/index price oracle baseline, liquidation MVP, funding settlement MVP, and reconciliation baseline exist.
- Outbox retry, max retry, DLQ replay, and manual compensation baselines exist.
- Kafka topic, Redis key schema, request/correlation id, audit log, and ops metrics baseline documents exist.
- Test folders have README indexes, and test cases use comments plus `@DisplayName` to explain test flow.

## What Is Not Production Complete

- The matching engine is still in-memory and does not yet have a durable command log, event log, snapshot, offset checkpoint, or replay.
- The per-symbol sequencer is only an in-process baseline; production deployment and failover rules are still missing.
- Order lifecycle events now have a durable event log and latest-state projection baseline; broader order/account replay and operational runbooks are still incomplete.
- The ledger now has a durable double-entry journal and replay path; audit retention, deeper replay validation, and operational controls are still incomplete.
- Funding, account risk snapshots, and manual liquidation now require mark/index price oracle input; risk tiers cover initial margin, maintenance margin, leverage, and stepped position caps. Production feed redundancy, price clamps, and liquidation scanning are still incomplete.
- Reconciliation now has persisted reports, a configurable scheduler policy, alert-route baseline, and event-store coverage checks.
- Outbox now uses a production durable MySQL store for outbox/DLQ records and has an operational replay/compensation runbook.
- MySQL, Redis, and Kafka transaction boundaries are not fully defined.
- Market data still needs durable sequence checkpoints, reconnect backfill, ticker/kline/trade-tape persistence.
- The WebSocket/SSE gateway still needs independent deployment, horizontal scaling, subscription authorization, heartbeat, rate limiting, and disconnect recovery.
- Polymarket order lifecycle, schema versioning, idempotent commands, and the user WebSocket worker are mostly still TODO.
- Metrics backend, distributed tracing export, dashboards, and alerting are incomplete.
- Production indexes, Flyway-only schema policy, archive policy, admin console, reporting, load testing, and compliance are not complete.

## Recommended Next Work

1. Build durable order / ledger / event schemas and lifecycle projections.
2. Evolve the matching engine into a replayable core with command log, event log, snapshot, and offset checkpoint.
3. Harden the mark/index price oracle with production feeds, then complete production pre-trade abuse controls and liquidation scanning.
4. Add reconciliation reports, scheduling, alert routing, and explicit MySQL / Redis / Kafka transaction boundaries.
5. Move outbox, market data, WebSocket gateway, and Polymarket workers from MVP baseline toward operable services.

## Reading Order

For a quick status read, start here, then read:

1. [todo.md](todo.md): full production-readiness checklist.
2. [technical.md](technical.md): technical documentation index.
3. [README.md](README.md): product and API overview.
4. [../../src/main/java/com/example/exchange/infra/matching/README.md](../../src/main/java/com/example/exchange/infra/matching/README.md): matching engine status.
