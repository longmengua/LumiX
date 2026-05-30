<!-- File purpose: quick snapshot of codebase completion, MVP capabilities, and production blockers. Chinese version: ../zh-TW/current-state.md. -->
# Current State

This document answers one question: how complete is this repository right now?

Short answer: this is a runnable and testable trading-core MVP, not a production-ready exchange. The core-v1 freeze checklist is now closed and post-v1 production hardening tasks are split out; the next step is to tag or hand off the bounded core-v1 baseline, then work through transaction boundaries and the other post-v1 blockers in order.

中文版本：[../zh-TW/current-state.md](../zh-TW/current-state.md)

## Completion Snapshot

The counts below come from the `[x]` / `[ ]` status in [todo.md](todo.md).

| Scope | Completed Baseline | Open Production Work | Reading |
| --- | ---: | ---: | --- |
| P0 Required | 31 | 12 | Core MVP capability exists, but many production blockers remain. |
| P1 Strongly Recommended | 8 | 14 | Operations, market data, Polymarket, and data governance are still early. |
| P2 Evolution | 0 | 5 | Admin, reporting, load testing, compliance, and rollout controls have not started. |
| Total | 39 | 31 | The project has a baseline, but production hardening is still the main body of work. |

## Current Priority Override

The next work should tag or hand off the baseline defined by [core-v1-release-checklist.md](core-v1-release-checklist.md). The freeze checklist, smoke runbook, and [post-v1 production hardening tasks](../tasks/post-v1/README.md) are complete. Do not expand web, Polymarket, reporting, compliance, or observability scope until core-v1 is tagged.

The frozen core-v1 baseline includes:

1. Replayable matching command/event log, snapshots, checkpoints, and replay validation.
2. Liquidation and ADL execution, including operator controls and audit events.
3. Bonus-credit / experience-fund accounting and turnover tracking.
4. Auditable ledger book and reconciliation exception workflow.
5. Market-maker quoting, inventory, kill switch, hedge interface, and hedge strategy baseline.

Polymarket worker split, WebSocket gateway scaling, and broader observability work remain important, but they are deferred for core v1.

## Capabilities You Can Reasonably Rely On

- Local MySQL, Redis, Kafka, and Kafka UI can be started with Docker Compose.
- Internal exchange order entry has an MVP chain: validation, pre-trade risk, in-memory matching, accounting updates, and event publishing.
- Matching behavior has deterministic tests for FIFO, post-only, self-match prevention, IOC/FOK, and insufficient market-order liquidity.
- An in-process per-symbol sequencer baseline serializes same-symbol matching operations within one process.
- Production deployment and failover rules for per-symbol sequencer ownership are documented.
- Matching state has an in-memory snapshot export/restore baseline that preserves resting order FIFO, command offset, event offset, and match sequence.
- Matching has in-memory command/event log and replay baselines that can rebuild state from a snapshot checkpoint in deterministic tests.
- Matching command/event logs, engine snapshots, and replay validation reports now have Flyway schema, JPA durable adapter baselines, and per-symbol offset checkpoints.
- Matching recovery orchestration can restore a symbol from latest snapshot plus command log, then persist the recovered snapshot and replay validation report.
- Matching sequencer lease service can acquire, renew, release, and increment per-symbol owner epochs on takeover.
- Matching sequencer write guard rejects missing lease, wrong owner, stale epoch, and expired lease before command writes.
- Matching command replay supports cancel-replace with a replacement order payload.
- Matching command/event log entries can persist sequencer owner id and epoch for fencing audit.
- Matching worker startup/renewal lifecycle can acquire configured symbol leases, run recovery, validate replay, retain owner/epoch readiness context, renew leases, remove readiness when renewal fails, and expose readiness inspection endpoints.
- Matching worker execution baseline can append lease-fenced commands for submit, cancel, amend, and cancel-replace before applying them to the engine, while preserving owner/epoch.
- Existing submit, cancel, amend, and cancel-replace's accounting-safe cancel + replacement-submit intake paths can use worker execution when the symbol has a ready owner context.
- `matching-worker.fence-legacy-routing` can reject old in-process fallback for configured symbols that are not worker-ready during cutover.
- `MatchingWorkerStartupListener` starts configured worker symbols after application readiness when `matching-worker.enabled=true`.
- Matching replay validation can compare replay output against an expected snapshot and report command-offset, event-offset, match-sequence, and book-level differences.
- Market data depth deltas now have durable sequence/checksum checkpoints, restart recovery of latest depth sequence, durable depth delta records, and a reconnect backfill endpoint for deltas after a known version. Trade tape has a durable restart-safe baseline.
- A wallet-ledger balanced posting baseline makes MVP fund movements traceable and testable.
- Accounting entries are split for order reserve, position margin, fee, rebate, realized PnL, funding, liquidation shortfall, deposit, and withdrawal.
- Bonus credit now has separate ledger postings for grant, consume, expiry, and clawback under `USER_BONUS_AVAILABLE`, does not change real cash account balance, and has grant-batch expiry/remaining tracking plus disabled-by-default expiry and campaign auto-clawback schedulers.
- Turnover has a durable read-model baseline derived from processed trade events with user, account, symbol, strategy, market-maker, order, match, sequence, quantity, price, and notional dimensions.
- Trial balance can be calculated from wallet ledger postings by asset/account code.
- Deposit and withdrawal have a state-machine baseline covering pending, confirmed, failed, reversed, and manual review; deposit callbacks replay by `externalRef`, manual-review transfers can be owner-claimed, and transfer reconciliation projections compare transfers with ledger refs.
- Account risk snapshot, persisted risk snapshot, pre-trade risk checks, uid+symbol order-entry frequency limits, risk tiers, global risk switches, mark/index price oracle baseline, liquidation MVP, funding settlement MVP, and reconciliation baseline exist.
- Liquidation decisions now publish audit data, and operator controls can halt liquidation or route it to manual review.
- Liquidation scanning can iterate open positions and trigger oracle-based liquidation decisions.
- ADL now has deterministic ranking, deleveraging-plan, forced-execution, durable queue storage, idempotent queue enqueue by `liquidationId`, queue-to-execution orchestration, operator claim/release, stuck-claim operator reporting, partial retry, and no-eligible-candidate retry baselines that reduce selected positions, write realized-PnL and socialized-loss ledger postings, publish audit events, and persist durable execution summary/idempotency records.
- Outbox retry, max retry, DLQ replay, and manual compensation baselines exist.
- Kafka topic, Redis key schema, request/correlation id, audit log, and ops metrics baseline documents exist.
- Redis hot-state keys now have final per-key-family TTL/archive rules, deletion preconditions, and authoritative rebuild sources documented for production maintenance.
- Historical orders, trades, ledger entries, Kafka events, and audit logs now have an archive strategy covering manifests, retention classes, delete preconditions, and restore rules.
- Order lifecycle projection now emits searchable `CORE_EVENT` structured log lines keyed by uid, orderId, clientOrderId, and symbol.
- Test folders have README indexes, and test cases use comments plus `@DisplayName` to explain test flow.

## What Is Not Production Complete

- Production worker routing has a documented deployment switch sequence, readiness inspection, rollback sequence, and focused smoke verification. The per-symbol sequencer still executes in an in-process engine, so broader disaster recovery and multi-process operational hardening remain.
- Order lifecycle events now have a durable event log and latest-state projection baseline; broader order/account replay and operational runbooks are still incomplete.
- The ledger now has a durable double-entry journal, bonus-credit account separation, configurable bonus consume eligibility, bonus expiry and campaign auto-clawback scheduler baselines, bonus user/campaign report and clawback APIs, turnover facts, turnover summary/drill-down queries, and replay path; audit retention, deeper replay validation, exportable campaign reporting, turnover reconciliation, and operational controls are still incomplete.
- Funding, account risk snapshots, and manual liquidation now require mark/index price oracle input; risk tiers cover initial margin, maintenance margin, leverage, and stepped position caps. Liquidation scanning can route open positions through oracle-based liquidation with halt/manual-review controls, batch limits, per-position failure isolation, and decision audit events. Production feed redundancy, price clamps, alert-backend delivery for stuck claims, and production insurance-fund capital movement records are still incomplete.
- Reconciliation now has persisted reports, a configurable scheduler policy, alert-route baseline, event-store coverage checks, trial-balance calculation, structured ledger replay comparison, issue workflow fields, admin issue workflow APIs, and workflow audit events. Daily finance reports remain incomplete.
- Market-maker hedging now has durable profile/risk-limit storage, admin profile APIs, hedge fill query APIs, venue fill callback ingestion with venue fill idempotent replay, manual and default-disabled scheduled hedge execution APIs, exposure aggregation, inventory-aware reduce-only hedge planning/execution, global hedge execution halt, per-run execution route cap policy, quote command validation, hedge venue adapter contract, retryable venue result classification, durable refId idempotency claim/result storage, unresolved hedge venue idempotency operator reporting, retry/backoff/throttle decorator baselines, standardized venue fill mapping, safe rejecting default adapter, hedging risk checks, slippage rejection, quote/hedge decision audit events, durable hedge decision/fill audit trails, and decision-vs-fill hedge reconciliation. Real venue adapters, venue lookup/reconciliation for uncertain outcomes, quote lifecycle integration, production callback authentication/verification, trade/ledger hedge reconciliation, scheduler/worker locking, and global limits remain incomplete.
- Outbox now uses a production durable MySQL store for outbox/DLQ records and has an operational replay/compensation runbook.
- Database indexing now has a Flyway baseline for durable order lifecycle projections/events, ledger entries/postings, outbox/DLQ/matching events, and prediction order/user-event queries; live order/position hot-state remains Redis-owned and still needs a durable indexing design before that TODO can close.
- MySQL, Redis, and Kafka transaction boundaries have a command-boundary/outbox baseline for order commands, liquidation, ADL execution, and hedge execution, but still need persistence-backed rollback tests and broader cross-store failure drills.
- Market data now has durable depth sequence checkpoints, reconnect backfill depth deltas, durable trade tape, durable ticker latest state, durable 1m klines, and disabled-by-default DB retention windows for high-volume depth/trade/kline history.
- The WebSocket/SSE gateway still needs independent deployment, horizontal scaling, subscription authorization, heartbeat, rate limiting, and disconnect recovery.
- Polymarket CLOB place now has a `clientRequestId` local idempotency baseline, CLOB cancel can use durable `commandId` records, CLOB cancel replays already-recorded cancel/uncertain statuses locally, reconcile can resolve uncertain cancel from remote CLOB status, sync/reconcile skip unchanged local writes, a state-machine guard prevents stale active CLOB payloads from downgrading local filled/settled terminal orders or matched size, approval reads have TTL cache coverage, session signer lifecycle guards cover expiration/revocation/abnormal-use warnings, user-channel callbacks no-op duplicate `eventKey` replays including save-race duplicates, and backend-observed RPC transactions have a durable command/txHash tracking envelope with unresolved outcome reporting; broader trade/settlement lifecycle, schema versioning, and the independently deployed user WebSocket worker are mostly still TODO.
- Metrics backend, distributed tracing export, dashboards, and alerting are incomplete.
- Archive exporter jobs, admin console, reporting, load testing, and compliance are not complete.

## Recommended Next Work

1. Tag or hand off the bounded core-v1 baseline.
2. Work through P0 production hardening via [post-v1 production hardening tasks](../tasks/post-v1/README.md).
3. Prioritize transaction boundaries, ADL forced execution, market data durability, and external API idempotency.
4. Defer new product surfaces until core-v1 is tagged.

## Reading Order

For a quick status read, start here, then read:

1. [core-v1-release-checklist.md](core-v1-release-checklist.md): release freeze boundary and gates.
2. [core-v1-smoke-runbook.md](core-v1-smoke-runbook.md): smoke verification flow.
3. [todo.md](todo.md): full production-readiness checklist.
4. [technical.md](technical.md): technical documentation index.
5. [../ai/code-map.md](../ai/code-map.md): compact agent code-map index.
