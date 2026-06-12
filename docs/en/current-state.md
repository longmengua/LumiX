<!-- File purpose: quick snapshot of codebase completion, MVP capabilities, and production blockers. Chinese version: ../zh-TW/current-state.md. -->
# Current State

This document answers one question: how complete is this repository right now?

Short answer: this is a runnable and testable trading-core MVP, not a production-ready exchange. The core-v1 freeze checklist is now closed, the P0 production baseline is complete, and post-v1 hardening task files are split out; the next step is to tag or hand off the bounded core-v1 baseline, then finish the remaining P1 production hardening blockers.

中文版本：[../zh-TW/current-state.md](../zh-TW/current-state.md)

## Completion Snapshot

The counts below come from the `[x]` / `[ ]` status in [todo.md](todo.md).

| Scope | Completed Baseline | Open Production Work | Reading |
| --- | ---: | ---: | --- |
| P0 Required | 43 | 0 | Core production baseline items are closed; post-v1 hardening still remains. |
| P1 Strongly Recommended | 22 | 0 | Strongly recommended production-hardening baselines are closed. |
| P2 Evolution | 0 | 5 | Admin market-config, risk-parameters, market-maker operations, and DLQ have static/API baselines; broader admin, reporting, load testing, compliance, and rollout controls remain incomplete. |
| Total | 65 | 5 | Production-hardening baselines are closed; P2 evolution work remains. |

## Current Priority Override

The next work should tag or hand off the baseline defined by [core-v1-release-checklist.md](core-v1-release-checklist.md). The freeze checklist and smoke runbook are complete, and the [post-v1 production hardening tasks](../tasks/post-v1/README.md) are split out. Do not expand web, Polymarket, reporting, compliance, or observability scope until core-v1 is tagged.

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
- The client exchange console loads selectable markets from `/api/markets`, only offers enabled admin-configured symbols, uses the authenticated session UID instead of an editable UID field, and displays animated order-book depth plus visible market-maker quote flow; public market refresh uses `/ws/market/{symbol}` before login, private open-order refresh uses `/ws/user/{uid}` after login, and the page falls back to one-second polling during disconnect/reconnect windows.
- Matching behavior has deterministic tests for FIFO, post-only, self-match prevention, IOC/FOK, and insufficient market-order liquidity.
- An in-process per-symbol sequencer baseline serializes same-symbol matching operations within one process.
- Production deployment and failover rules for per-symbol sequencer ownership are documented.
- Matching state has an in-memory snapshot export/restore baseline that preserves resting order FIFO, command offset, event offset, and match sequence.
- Matching has in-memory command/event log and replay baselines that can rebuild state from a snapshot checkpoint in deterministic tests.
- Matching command/event logs, engine snapshots, and replay validation reports now have Flyway schema, JPA durable adapter baselines, and per-symbol offset checkpoints.
- Matching recovery orchestration can restore a symbol from latest snapshot plus command log, assert recovered open orders in restore drills, then persist the recovered snapshot and replay validation report.
- Matching sequencer lease service can acquire, renew, release, and increment per-symbol owner epochs on takeover.
- Matching sequencer write guard rejects missing lease, wrong owner, stale epoch, and expired lease before command writes.
- Matching command replay supports cancel-replace with a replacement order payload.
- Matching command/event log entries can persist sequencer owner id and epoch for fencing audit.
- Matching worker startup/renewal lifecycle can acquire configured symbol leases, run recovery, validate replay, retain owner/epoch readiness context, renew leases, remove readiness when renewal fails, and expose readiness inspection endpoints.
- Matching worker execution baseline can append lease-fenced commands for submit, cancel, amend, and cancel-replace before applying them to the engine, while preserving owner/epoch.
- Existing submit, cancel, amend, and cancel-replace's accounting-safe cancel + replacement-submit intake paths can use worker execution when the symbol has a ready owner context.
- `matching-worker.fence-legacy-routing` can reject old in-process fallback for configured symbols that are not worker-ready during cutover.
- `MatchingWorkerStartupListener` starts configured worker symbols after application readiness when `matching-worker.enabled=true`.
- Matching replay validation can compare replay output against an expected snapshot and report command-offset, event-offset, match-sequence, book-level differences, and multi-symbol interleaved-offset coverage.
- Market data depth deltas now have durable sequence/checksum checkpoints, restart recovery of latest depth sequence, durable depth delta records, and a reconnect backfill endpoint for deltas after a known version. Trade tape has a durable restart-safe baseline plus `afterTs`/`afterMatchId` replay cursor support.
- A wallet-ledger balanced posting baseline makes MVP fund movements traceable and testable.
- Accounting entries are split for order reserve, position margin, fee, rebate, realized PnL, funding, liquidation shortfall, deposit, and withdrawal.
- Bonus credit now has separate ledger postings for grant, consume, expiry, and clawback under `USER_BONUS_AVAILABLE`, does not change real cash account balance, and has grant-batch expiry/remaining tracking, user/campaign report APIs, campaign export rows, operator clawback, plus disabled-by-default expiry and campaign auto-clawback schedulers.
- Turnover has a durable read-model baseline derived from processed trade events with user, account, symbol, first-class order strategy/market-maker tags, order, match, sequence, quantity, price, and notional dimensions, plus export rows and tag-aware trade/ledger reconciliation.
- Trial balance can be calculated from wallet ledger postings by asset/account code.
- Deposit and withdrawal have a state-machine baseline covering pending, confirmed, failed, reversed, and manual review; deposit callbacks replay by `externalRef`, manual-review transfers can be owner-claimed, and transfer reconciliation projections compare transfers with ledger refs.
- Account risk snapshot, persisted risk snapshot, pre-trade risk checks, uid+symbol order-entry frequency limits, risk tiers, global risk switches, mark/index price oracle baseline, liquidation MVP, funding settlement MVP, and reconciliation baseline exist.
- Liquidation decisions now publish audit data, and operator controls can halt liquidation or route it to manual review.
- Liquidation scanning can iterate open positions and trigger oracle-based liquidation decisions.
- ADL now has deterministic ranking, deleveraging-plan, forced-execution, durable queue storage, idempotent queue enqueue by `liquidationId`, queue-to-execution orchestration, operator claim/release, stuck/open queue alert reporting, stuck-claim operator reporting and runbook, recent execution reporting, insurance/shortfall reconciliation, durable insurance-fund movement records, partial retry with restart coverage, and no-eligible-candidate retry baselines that reduce selected positions, write realized-PnL and socialized-loss ledger postings, publish audit events, and persist durable execution summary/idempotency records.
- Outbox retry, max retry, DLQ replay, and manual compensation baselines exist.
- Kafka topic, Redis key schema, request/correlation id, audit log, and ops metrics baseline documents exist.
- Redis hot-state keys now have final per-key-family TTL/archive rules, deletion preconditions, and authoritative rebuild sources documented for production maintenance.
- Historical orders, trades, ledger entries, Kafka events, and audit logs now have an archive strategy covering manifests, retention classes, delete preconditions, and restore rules.
- Order lifecycle projection now emits searchable `CORE_EVENT` structured log lines keyed by uid, orderId, clientOrderId, and symbol.
- Test folders have README indexes, and test cases use comments plus `@DisplayName` to explain test flow.

## What Is Not Production Complete

- Production worker routing and disaster recovery now have documented deployment switch sequence, worker takeover, reconnect/session replay semantics, restore smoke commands, account/position consistency validation, readiness inspection, rollback sequence, and focused smoke verification. The per-symbol sequencer still executes in an in-process engine, so broader multi-process operational hardening remains.
- Order lifecycle events now have a durable event log and latest-state projection baseline; broader order/account replay and operational runbooks are still incomplete.
- The ledger now has a durable double-entry journal, SQL-enforced wallet ledger invariants, hash-chain tamper evidence, archive eligibility, archive manifests, restore smoke, archived date-range replay validation, immutable archive delete guard, bonus-credit account separation, configurable bonus consume eligibility, bonus expiry and campaign auto-clawback scheduler baselines, bonus user/campaign report/export and clawback APIs, turnover facts, first-class strategy/market-maker order tags, turnover summary/drill-down/export queries, match-level turnover-vs-trade-tape reconciliation, disabled-by-default recent-window turnover trade/ledger-ref reconciliation, and replay path; broader operational controls are still incomplete.
- Funding, account risk snapshots, and manual liquidation now require mark/index price oracle input; risk tiers cover initial margin, maintenance margin, leverage, and stepped position caps. Liquidation scanning can route open positions through oracle-based liquidation with halt/manual-review controls, batch limits, per-position failure isolation, decision audit events, ADL alert reporting, and insurance-fund movement records. Production feed redundancy and price clamps are still incomplete.
- Reconciliation now has persisted reports, a configurable scheduler policy, alert-route baseline, event-store coverage checks, durable ledger hash-chain tamper-evidence, SQL-enforced wallet ledger invariants, trial-balance calculation and daily snapshot persistence, structured ledger replay comparison, issue workflow fields, admin issue workflow APIs, workflow audit events, a durable-ledger daily finance report baseline, disabled-by-default finance category exporter job, finance category exports for fee/funding/liquidation/bonus/transfer, ledger archive/delete eligibility checks, manifest restore smoke, archived date-range replay validation, and an unbalanced-report operator runbook.
- Market-maker hedging now has durable profile/risk-limit storage, admin profile APIs, hedge fill query APIs with bounded limits, venue fill callback ingestion with venue fill idempotent replay and optional HMAC/timestamp verification, manual and default-disabled scheduled hedge execution APIs with bounded safe ref prefixes, durable scheduled-worker lock, operator approval token gate, manual hedge execution API frequency limiting, effectful endpoint audit fields for operator identity and approval token outcome, exposure aggregation, inventory-aware reduce-only hedge planning/execution, global hedge execution halt, per-run execution route and notional cap policy shared across enabled-profile batches, quote command validation, quote API frequency limiting, stale quote cleanup, post-only internal order placement, durable active quote state/operator lookup, per-side quote version metadata, active quote reload coverage, quote/open-order reconciliation, and default-disabled fail-closed quote repair baselines, hedge venue adapter contract, real venue signed-request/lookup HTTP transport, venue outcome lookup contract for uncertain submit reconciliation, retryable venue result classification, durable refId idempotency claim/result storage, unresolved hedge venue idempotency operator reporting and reconcile trigger, retry/backoff/throttle decorator baselines, standardized venue fill mapping, safe rejecting default adapter, hedging risk checks, slippage rejection, quote/hedge decision audit events, durable hedge decision/fill audit trails with internal trade and ledger refs, decision-vs-fill hedge reconciliation, and trade/ledger ref issue reporting. Automated hedge reconciliation repair jobs remain incomplete.
- Outbox now uses a production durable MySQL store for outbox/DLQ records and has an operational replay/compensation runbook.
- Database indexing now has a Flyway baseline for durable order lifecycle projections/events, ledger entries/postings, outbox/DLQ/matching events, prediction order/user-event queries, and the `position_lifecycle_projection` live-position mirror schema/indexes. Live order SQL mirror design uses `order_lifecycle_projection`, and archive exporter skeletons exist for historical order/trade/ledger export plans. Object-storage archive writers, delete jobs, and live-position projection update/rebuild wiring are still incomplete.
- Operations metrics now expose in-process matching latency, rejection rate, fill rate, DB operation latency, Redis operation latency, and Kafka consumer lag counters through both `/api/ops/metrics` and Micrometer Prometheus export at `/actuator/prometheus`.
- MySQL, Redis, and Kafka transaction boundaries have a command-boundary/outbox baseline for order commands, liquidation, ADL execution, and hedge execution, rollback coverage for order-place outbox insert failure, cancel ledger-release failure, and hedge audit/outbox failure, a cross-store MySQL/Redis/Kafka failure drill, and an outbox/domain-state consistency recovery report.
- Market data now has durable depth sequence checkpoints, reconnect backfill depth deltas, durable trade tape, trade replay cursors, durable ticker latest state, durable 1m klines, and disabled-by-default DB retention windows for high-volume depth/trade/kline history.
- The WebSocket/SSE gateway now has a heartbeat contract, disabled-by-default heartbeat scheduler, private user stream subscription authorization for SSE/WebSocket, per-client stream subscription rate limiting, a depth/trade recovery cursor contract, `push-gateway.runtime.*` role/drain controls for MONOLITH/GATEWAY deployments, a runtime status endpoint at `GET /api/ops/push-gateway/status`, and a horizontal-scaling deployment runbook.
- Polymarket CLOB place now has a `clientRequestId` local idempotency baseline, CLOB cancel can use durable `commandId` records, CLOB cancel replays already-recorded cancel/uncertain statuses locally, reconcile can resolve uncertain cancel from remote CLOB status, sync/reconcile skip unchanged local writes, a documented local/CLOB/trade/settlement transition matrix prevents stale active or terminal downgrade payloads from downgrading local filled/settled terminal orders or matched size, lets settlement/redeem events advance matched or filled orders to settled, routes user-channel trade matches into the local matched lifecycle, user-channel trade payloads persist matched lifecycle and lastTradeId into the local `PredictionPolymarketOrder` projection, user WebSocket worker checkpoint/replay has a durable wallet-scoped baseline plus `polymarket.ws.user-worker-*` runtime identity, startup bounded replay, status checkpoint visibility, and manual replay control, approval reads have TTL cache coverage, session signer lifecycle guards cover expiration/revocation/abnormal-use warnings, user-channel callbacks no-op duplicate `eventKey` replays including save-race duplicates, backend-observed RPC transactions have a durable command/txHash tracking envelope with unresolved outcome reporting, and Gamma/CLOB responses now have versioned schema reports for remote-field drift.
- Alert backend integration now has `OperationalAlert`, `AlertDispatchService`, and `OkHttpAlertTransport` wired through disabled-by-default `alerts.backend.*` config; alert payloads carry severity, route, entity id, runbook, details, and request/correlation ids when available, and skipped/failed dispatch does not mutate trading state. Tracing export/sampling policy config exists under `tracing.export.*`, Micrometer Tracing OTLP export is wired through Actuator management properties, and tracing dashboard docs cover the first Grafana/Tempo panels.
- Admin market-config, risk-parameters, market-maker operations, and DLQ have static/API baselines. Market-maker operations now include profile creation, strategy risk limits, active quote state, hedge reconciliation/fills, idempotency status, and guarded manual hedge execution through existing `/api/market-maker` APIs; broader reporting, load testing, rollout, and compliance features are not complete.

## Recommended Next Work

1. Tag or hand off the bounded core-v1 baseline.
2. Continue P2 only after the core-v1 baseline is tagged or explicitly handed off.

## Reading Order

For a quick status read, start here, then read:

1. [core-v1-release-checklist.md](core-v1-release-checklist.md): release freeze boundary and gates.
2. [core-v1-smoke-runbook.md](core-v1-smoke-runbook.md): smoke verification flow.
3. [todo.md](todo.md): full production-readiness checklist.
4. [technical.md](technical.md): technical documentation index.
5. [../ai/code-map.md](../ai/code-map.md): compact agent code-map index.
