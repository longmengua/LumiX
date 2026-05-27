<!-- File purpose: quick snapshot of codebase completion, MVP capabilities, and production blockers. Chinese version: ../zh-TW/current-state.md. -->
# Current State

This document answers one question: how complete is this repository right now?

Short answer: this is a runnable and testable trading-core MVP, not a production-ready exchange. The active roadmap now prioritizes the core exchange/matching kernel first: replayable matching, ADL, bonus credit, turnover, auditable books/reconciliation, and market-maker hedging.

中文版本：[../zh-TW/current-state.md](../zh-TW/current-state.md)

## Completion Snapshot

The counts below come from the `[x]` / `[ ]` status in [todo.md](todo.md).

| Scope | Completed Baseline | Open Production Work | Reading |
| --- | ---: | ---: | --- |
| P0 Required | 26 | 17 | Core MVP capability exists, but many production blockers remain. |
| P1 Strongly Recommended | 6 | 16 | Operations, market data, Polymarket, and data governance are still early. |
| P2 Evolution | 0 | 5 | Admin, reporting, load testing, compliance, and rollout controls have not started. |
| Total | 32 | 38 | The project has a baseline, but production hardening is still the main body of work. |

## Current Priority Override

The next work should stay on the core exchange kernel until it is complete enough for production-style trading tests:

1. Replayable matching command/event log, snapshots, checkpoints, and replay validation.
2. Liquidation and ADL execution, including operator controls and audit events.
3. Bonus-credit / experience-fund accounting and turnover tracking.
4. Auditable ledger book and reconciliation exception workflow.
5. Market-maker quoting, inventory, kill switch, hedge interface, and hedge strategy baseline.

Polymarket worker split, WebSocket gateway scaling, and broader observability work remain important, but they should not outrank this core-kernel lane.

## Capabilities You Can Reasonably Rely On

- Local MySQL, Redis, Kafka, and Kafka UI can be started with Docker Compose.
- Internal exchange order entry has an MVP chain: validation, pre-trade risk, in-memory matching, accounting updates, and event publishing.
- Matching behavior has deterministic tests for FIFO, post-only, self-match prevention, IOC/FOK, and insufficient market-order liquidity.
- An in-process per-symbol sequencer baseline serializes same-symbol matching operations within one process.
- Production deployment and failover rules for per-symbol sequencer ownership are documented.
- Matching state has an in-memory snapshot export/restore baseline that preserves resting order FIFO, command offset, event offset, and match sequence.
- Matching has in-memory command/event log and replay baselines that can rebuild state from a snapshot checkpoint in deterministic tests.
- Matching command/event logs, engine snapshots, and replay validation reports now have Flyway schema, JPA durable adapter baselines, and per-symbol offset checkpoints.
- Matching replay validation can compare replay output against an expected snapshot and report command-offset, event-offset, match-sequence, and book-level differences.
- A wallet-ledger balanced posting baseline makes MVP fund movements traceable and testable.
- Accounting entries are split for order reserve, position margin, fee, rebate, realized PnL, funding, liquidation shortfall, deposit, and withdrawal.
- Deposit and withdrawal have a state-machine baseline covering pending, confirmed, failed, reversed, and manual review.
- Account risk snapshot, persisted risk snapshot, pre-trade risk checks, risk tiers, global risk switches, mark/index price oracle baseline, liquidation MVP, funding settlement MVP, and reconciliation baseline exist.
- Outbox retry, max retry, DLQ replay, and manual compensation baselines exist.
- Kafka topic, Redis key schema, request/correlation id, audit log, and ops metrics baseline documents exist.
- Test folders have README indexes, and test cases use comments plus `@DisplayName` to explain test flow.

## What Is Not Production Complete

- The matching engine still lacks startup/worker-takeover recovery orchestration and distributed sequencer lease / epoch fencing.
- The per-symbol sequencer is only implemented as an in-process baseline; production distributed lease, epoch fencing, and worker routing are still missing.
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
- Production indexes, archive policy, admin console, reporting, load testing, and compliance are not complete.

## Recommended Next Work

1. Evolve the matching engine into a replayable core with command log, event log, snapshot, offset checkpoint, and replay validation.
2. Complete liquidation and ADL execution paths with operator controls.
3. Add bonus-credit / experience-fund accounting and turnover tracking.
4. Harden ledger reconciliation into an auditable book with exception workflow.
5. Build market-maker interfaces and hedging strategy baseline.

## Reading Order

For a quick status read, start here, then read:

1. [todo.md](todo.md): full production-readiness checklist.
2. [technical.md](technical.md): technical documentation index.
3. [README.md](README.md): product and API overview.
4. [../ai/code-map.md](../ai/code-map.md): compact agent code-map index.
5. [../../src/main/java/com/example/exchange/infra/matching/README.md](../../src/main/java/com/example/exchange/infra/matching/README.md): matching engine status.
