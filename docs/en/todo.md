<!-- File purpose: English production-readiness checklist. Other languages are listed in the repository root README.md. -->
# Production TODO

This checklist focuses on the work needed to move the current MVP toward production. Core-v1 freeze is closed; the next core production lane is [post-v1 production hardening tasks](../tasks/post-v1/README.md). Fine-grained progress for the broad checklist items is tracked in [production-readiness fine tasks](../tasks/production-readiness/README.md).

Documentation categories: [Product Documentation](README.md) / [Technical Documentation](technical.md) / TODO Documentation

## Active Freeze Work

- [x] Close [core-v1-release-checklist.md](core-v1-release-checklist.md).
- [x] Run [core-v1-smoke-runbook.md](core-v1-smoke-runbook.md).
- [x] Fix only compile/test/checklist gaps discovered during freeze verification.
- [x] Defer web apps, Polymarket production worker split, broad reporting, compliance, and observability expansion until after core-v1 is tagged.

## P0 Required

### Core Exchange Kernel Priority Lane

- [x] Finish the replayable matching core: durable command log, event log, snapshots, offset checkpoints, and deterministic replay validation.
  - Baseline done: durable command/event logs, durable snapshots, offset checkpoints, owner/epoch fencing audit fields, deterministic replay validation reports, worker startup/takeover recovery orchestration, restore drill coverage for recovered open orders, and multi-symbol replay validation for interleaved command offsets.
- [x] Complete production ADL: queue ranking, forced deleveraging execution, audit events, insurance-fund interaction, and operator controls.
  - Baseline done: deterministic ranking/planning, liquidation decision audit, operator halt/manual-review hooks, forced-execution service for position reduction, ledger postings, audit events, durable execution summary/idempotency records, recent execution report API, durable ADL queue store, idempotent ADL queue enqueue by `liquidationId`, queue-to-execution orchestration, operator claim/release guard, stuck/open ADL alert report API, stuck-claim operator report and runbook, partial-execution retry semantics including restart coverage, no-eligible-candidate retry semantics, ADL insurance/shortfall reconciliation, and durable insurance-fund movement records.
- [x] Add bonus-credit / experience-fund accounting with separate ledger accounts, eligibility rules, consumption priority, expiry, clawback, and reporting.
  - Baseline done: separate bonus ledger account, grant/consume/expiry/clawback postings, cash-balance isolation, grant-batch remaining tracking, disabled-by-default expiry scanner, configurable consume eligibility gate, disabled-by-default campaign auto-clawback policy, user/campaign bonus-credit report APIs, exportable campaign report rows, and operator clawback API.
- [x] Add turnover tracking for user, account, symbol, strategy, and market-maker dimensions, with ledger/trade reconciliation.
  - Baseline done: durable turnover records emitted from processed trades, first-class strategy/market-maker order tags through order placement and lifecycle projection, turnover summary, limited drill-down, export rows by uid/symbol/strategy/market-maker, match-level trade-tape reconciliation APIs, and disabled-by-default recent-window reconciliation for uid+match trade tape / ledger-ref checks.
- [x] Harden ledger reconciliation into an auditable accounting book: immutable journals, trial balance, replay comparison, exception workflow, and finance reports.
  - Baseline done: durable journal hash-chain tamper-evidence, SQL-enforced wallet ledger entry/posting invariants, trial-balance calculation and daily snapshot persistence, structured replay comparison, reconciliation issue workflow fields/admin APIs, workflow audit events, durable-ledger daily finance report by reason/asset/account code, disabled-by-default finance category exporter job, category exports for fee/funding/liquidation/bonus/transfer, ledger archive/delete eligibility checks, ledger archive manifest checksums, restore smoke checks, replay validation for archived date ranges, and operator runbook for unbalanced reports.
- [x] Build market-maker interfaces for quoting, inventory, risk limits, kill switch, and hedging order routing.
  - Baseline done: durable profile/risk-limit storage, admin profile APIs, exposure aggregation, quote command validation, stale quote cleanup, post-only quote order placement, durable active quote state/operator lookup, per-side quote version metadata, active quote reload coverage, quote/open-order reconciliation, default-disabled fail-closed quote repair, kill switch, slippage control, hedge venue contract, safe rejecting adapter, real venue signed-request/lookup HTTP transport, quote/hedge decision audit events, durable hedge decision audit trails with internal trade refs, hedge fill audit persistence with ledger refs, and hedge trade/ledger reconciliation issues.
- [x] Build market-maker hedging strategy baseline: exposure aggregation, hedge venue adapter interface, execution policy, slippage controls, and hedge audit trail.
  - Baseline done: exposure aggregation, hedge venue adapter interface, real venue signed-request/lookup HTTP transport, per-run execution route/notional cap policy shared across enabled-profile batches, slippage controls, durable scheduled-worker lock, operator approval token gate, bounded operator queries/ref prefixes, optional venue callback HMAC/timestamp verification, uncertain outcome venue lookup/reconcile contract, stale quote cleanup, post-only quote order placement, durable active quote state/operator lookup, per-side quote version metadata, quote/open-order reconciliation, default-disabled fail-closed quote repair, durable hedge decision audit trail with internal trade refs, fill audit persistence with ledger refs, and trade/ledger reconciliation issues.

### Trading and Matching

- [x] Evolve the in-memory matching engine into a replayable matching core with command log, event log, snapshot, and offset checkpoint.
- [x] Add an in-process per-symbol sequencer baseline so matching operations for the same symbol are serialized.
- [x] Define production deployment and failover rules for the per-symbol sequencer to prevent multiple instances from processing the same symbol concurrently.
- [x] Publish order lifecycle events for created, accepted, updated, rejected, canceled, expired, and filled states.
- [x] Persist and operationalize order lifecycle events with durable storage, schema versioning, replay, and query projections.
- [x] Add REST/WebSocket baselines for amend order, cancel replace, bulk cancel, and cancel on disconnect.
- [x] Add durable command logs, stronger cancel-replace atomicity modes, and reconnect/session semantics for exchange-standard commands.
  - Baseline done: durable matching command/event logs, worker fencing, cancel-replace command replay, cancel-replace reserve-release/replacement rollback coverage, cancel-on-disconnect connection resume semantics, and DR runbook reconnect/session replay guidance.
- [x] Enforce tick size, lot size, min notional, price band, max order size, and max open orders in pre-trade checks.
- [x] Make rejection semantics explicit for insufficient MARKET liquidity, unfilled IOC/FOK, POST_ONLY taking liquidity, and REDUCE_ONLY exceeding reducible position size.

### Accounting and Funds

- [x] Add a balanced wallet-ledger posting baseline so balance changes are traceable and reconcilable in the MVP.
- [x] Build the complete production double-entry ledger schema and replay path.
- [x] Split order reserve, position margin, fee, rebate, realized PnL, funding, liquidation shortfall, deposit, and withdrawal into explicit accounting entries.
- [x] Harden accounting entries with production database constraints, audit retention, and replay validation.
  - Baseline done: SQL-enforced wallet ledger entry/posting constraints, durable hash-chain tamper evidence, daily/category finance reports, trial-balance snapshots, replay comparison, archive eligibility, archive manifest checksums, restore smoke, archived date-range replay validation, and immutable ledger archive delete guard that blocks hot-path delete unless eligibility, manifest, restore smoke, and replay validation all pass.
- [x] Add `/api/margin/risk` for frozen funds, available balance, total equity, maintenance margin, and risk ratio snapshots.
- [x] Persist daily account risk snapshots and fully replace trade/ticker fallback marks with independent mark/index oracle inputs.
- [x] Add an all-account reconciliation baseline that scans the maintained account index plus open-position index and reports account, position margin, and ledger-balance issues.
- [x] Add persisted reconciliation reports, scheduling policy, alert routing, and event-store coverage.
- [x] Add a Redis-backed deposit/withdrawal state-machine baseline for pending, confirmed, failed, reversed, and manual review transfer states.
- [x] Add chain/bank callbacks, manual-review workflow ownership, and transfer reconciliation projections.
  - Baseline done: deposit callbacks use `externalRef` idempotency to replay duplicates without double ledger posting, manual-review transfers can be claimed by an owner, and transfer reconciliation projections compare each transfer with wallet ledger refs.

### Risk

- [x] Integrate mark price / index price oracles so liquidation and funding do not depend on trade price or arbitrary input.
- [x] Add symbol risk baseline settings for max leverage, maintenance margin rate, max position notional, and max open orders.
- [x] Add full risk tiers with initial margin rate and stepped position limits.
- [x] Add pre-trade risk checks for balance, leverage, position, exposure, price deviation, and client order id deduplication.
- [x] Add production frequency limits and broader abuse controls to pre-trade risk checks.
  - Baseline done: configurable uid+symbol fixed-window order-entry frequency limit rejects burst orders before reserve/matching side effects; default is disabled, and multi-instance production can replace the local counter with Redis/gateway shared counting.
- [x] Add a liquidation MVP with trigger, close, insurance fund, ADL, and audit event coverage.
- [x] Add production liquidation scanning, execution routing, and operational controls.
  - Baseline done: scanner iterates open positions with oracle-based liquidation routing, operator halt/manual-review controls, decision audit events, configurable scan batch size, and per-position failure isolation so one bad symbol/config does not stop the batch.
- [x] Add global risk switches for reduce-only mode, order-entry halt, withdrawal halt, and per-symbol suspension.

### Reliability and Consistency

- [x] Add outbox retry backoff, max retry count, DLQ replay, and manual compensation workflow baseline.
- [x] Move outbox to production durable storage and add manual compensation runbooks.
- [x] Document Kafka topic partition keys, retention, compaction, schema versions, and consumer-group strategy.
- [x] Add shared HTTP timeout, retry, circuit breaker, and rate-limit baseline for external API calls.
- [x] Verify timeout, retry, circuit breaker, rate limit, and idempotency coverage for every external API call.
  - Baseline done: external API inventory, shared HTTP timeout/retry/circuit/rate-limit config, durable hedge venue submit idempotency envelope using `refId`, hedge venue fill callback replay using `venueOrderId + venueFillId`, optional hedge venue fill callback HMAC/timestamp verification, unresolved hedge venue idempotency operator report and lookup reconcile trigger, CLOB place local idempotency using `clientRequestId`, durable CLOB cancel `commandId`, CLOB cancel local replay for already-recorded cancel/uncertain statuses, reconcile resolution for uncertain cancel, CLOB sync/reconcile no-op local replay for unchanged payloads, Polymarket user-channel callback replay/race idempotency using `eventKey`, approval read TTL cache coverage, and durable backend-observed RPC transaction tracking with unresolved outcome reporting.
- [x] Define transaction boundaries for core writes; MySQL, Redis, and Kafka must not be assumed to be automatically consistent.
  - Baseline done: command transaction boundaries now wrap order place/cancel/amend/cancel-replace, manual liquidation, ADL forced execution, and hedge execution; outbox rows are saved in the command transaction and external publish is deferred until `afterCommit`; rollback coverage now includes order-place outbox insert failure, cancel ledger-release failure, cancel-replace reserve-release/replacement reserve failure, and hedge audit/outbox failure; cross-store MySQL/Redis/Kafka failure drill and outbox/domain-state consistency recovery report are available.
- [x] Add MVP snapshot + event replay recovery entry points.
- [x] Build production disaster recovery for matching, orders, accounts, and positions.
  - Baseline done: production DR runbook for matching/order/account/position restore, worker takeover steps, restore smoke command list, outbox/domain-state consistency report, and account/position consistency validation report after restore.

### Security

- [x] Add session signer lifecycle controls: expiration, revocation, audit, and abnormal-use detection.
  - Baseline done: session use rejects inactive/expired records, expired use marks the record `EXPIRED`, revoke-all now covers both `PENDING` and `ACTIVE` sessions so a signed-but-unconfirmed signer cannot become active after wallet-wide revoke, and limit-breach/invalid-use warnings provide an audit trail for abnormal-use review.

## P1 Strongly Recommended

### Market Data

- [x] Add REST/SSE depth delta with monotonic version and CRC32 checksum for snapshot + delta validation.
- [x] Add durable sequence checkpoints and reconnect backfill for incremental order book streams.
  - Baseline done: durable depth-delta sequence/checksum checkpoints, duplicate/out-of-order checkpoint ignoring, startup recovery of latest depth sequence, durable depth delta records, `GET /api/market-data/{symbol}/depth-deltas?afterVersion=...` reconnect backfill, durable trade tape records, durable ticker latest-state records, durable 1m kline records, and disabled-by-default DB retention windows.
- [x] Define retention/archive policy for high-volume market-data depth, trade, and kline history.
  - Baseline done: DB retention job purges depth delta, trade tape, and 1m kline history by independent windows; production archive export/storage remains a broader ops task.
- [ ] Deploy WebSocket/SSE gateway independently with horizontal scaling, subscription authorization, heartbeat, rate limiting, and disconnect recovery.
  - Baseline progress: gateway heartbeat contract emits `gateway.heartbeat` to SSE/WebSocket channels with channel and timestamp payload, removes closed WebSocket sessions, and has disabled-by-default scheduler config. Private user SSE/WebSocket streams now require API key or Bearer credentials when `api-auth.enabled=true`; admin principals can subscribe for operations, while user principals need matching uid ownership plus stream read scope. SSE/WebSocket stream subscription attempts now pass through a per-client fixed-window limiter under `push-gateway.rate-limit.*`. Clients can read `GET /api/market-data/{symbol}/recovery-cursor`, replay depth with `afterVersion`, and replay trades with `afterTs` plus `afterMatchId`. [Market data gateway scaling](market-data-gateway-scaling.md) documents independent gateway role, broadcast fanout, load-balancer draining, shared rate-limit options, heartbeat policy, and rollback.
- [x] Add market-maker / liquidity-provider API hardening and rate-limit policies after the P0 market-maker interface baseline is complete.
  - Baseline done: `POST /api/market-maker/quotes` now has a configurable fixed-window frequency limit under `market-maker.api.quote-rate-limit.*`, keyed by client, market-maker id, and symbol before quote replacement side effects run. Manual hedge execution endpoints now have a configurable fixed-window frequency limit under `market-maker.api.hedge-execution-rate-limit.*`, keyed by client and execution scope before external venue routing can run. Effectful quote and manual hedge execution endpoints emit `MARKET_MAKER_ENDPOINT_AUDIT` logs with operator subject, credential type, optional `X-Operator-Id`, request id, endpoint/resource, result/reason, and approval token outcome without logging the token value.

### Polymarket Integration

- [x] Build a Polymarket order state machine that tracks local order, CLOB order, trade, and settlement lifecycle.
  - Baseline done: [Polymarket order transition matrix](polymarket-order-transition-matrix.md) defines local/CLOB/trade/settlement state columns and allowed transitions. `PolymarketOrderStateMachine` routes user-channel trade events through the matrix, promotes matched trade events into local matched lifecycle, lets settlement/redeem events advance matched or filled local orders to settled, and preserves settled/filled terminal status against later stale active or terminal downgrade events.
- [x] Version Gamma/CLOB response schemas to reduce breakage when remote fields change.
  - Baseline done: Gamma `/events` and `/markets` responses are validated through versioned schema reports before DTO parsing, Gamma event/market DTOs ignore unknown remote fields, and CLOB order-operation responses record `clob.order-operations.v1` metadata while warning on incompatible shapes.
- [x] Make CLOB place, cancel, sync, and reconcile commands idempotent.
  - Baseline done: place can use `clientRequestId`; cancel can use durable `commandId`; cancel locally replays already-recorded cancel/uncertain statuses; reconcile can resolve uncertain cancel from remote CLOB status; sync/reconcile skip unchanged local writes; stale active CLOB payloads cannot downgrade local filled/settled terminal orders or matched size; user-channel trade payloads now resolve order/trade ids from top-level fields or payload and persist matched lifecycle/lastTradeId into the local `PredictionPolymarketOrder` projection; settlement terminal downgrade tests cover filled-to-settled progression and settled downgrade rejection.
  - Baseline done: place supports `clientRequestId` duplicate replay, payload conflict rejection, and uncertain local-order retry blocking.
- [ ] Deploy the user WebSocket service independently with reconnect, checkpoint, event deduplication, persistence, and replay.
  - Baseline progress: user WebSocket gateway publishes user-channel messages to Kafka, persists a durable wallet-scoped checkpoint after publish/replay, and can replay persisted `prediction_polymarket_ws_event` rows after the checkpoint for restart recovery tests. Remaining: split/deploy the worker independently and add operational checkpoint controls.
- [x] Add cache and expiry policy for allowance / approval checks to avoid overloading RPC endpoints.
  - Baseline done: ERC20 allowance and ERC1155 approval reads use owner/contract scoped TTL cache, owner-scoped clear, full-cache clear, and expiry refresh coverage.

### Database and Storage

- [ ] Add production indexes for orders, positions, ledger, events, and prediction orders.
  - Baseline done: Flyway `V12__production_query_indexes.sql` adds query indexes for durable order lifecycle projections/events, ledger entries/postings, outbox/DLQ/matching events, and prediction orders/user events. [Live order SQL mirror](live-order-sql-mirror.md) design chooses `order_lifecycle_projection` as the durable live-order mirror, [live position SQL mirror](live-position-sql-mirror.md) defines future `position_lifecycle_projection`, and the archive exporter skeleton covers historical order/trade/ledger export plans. Remaining: implement the future position mirror schema.
- [x] Document Redis key schema, namespace prefix, versioning, and migration strategy.
- [x] Add final TTL/archive rules for Redis hot-state keys.
  - Baseline done: `docs/en/redis-key-schema.md` defines per-key-family production TTL, archive/delete rule, and authoritative rebuild source for account, position, order, snapshot, ledger, outbox/DLQ, and idempotency keys.
- [x] Use Flyway as the single production schema manager; do not rely on Hibernate `ddl-auto=update`.
- [x] Add archive strategy for historical orders, trades, ledger entries, Kafka events, and audit logs.
  - Baseline done: `docs/en/archive-strategy.md` defines hot/archive sources, minimum payloads, retention classes, manifests, delete rules, and restore rules; `ArchiveExporterService` / `ArchiveExporterScheduler` provide a disabled-by-default historical order/trade/ledger export-plan skeleton. Remaining: object-storage writers, delete jobs, and broader restore smoke tests.

### Observability

- [x] Add `/api/ops/metrics` baseline for order status, order latency, cancel count, and trade-event count.
- [ ] Add metrics backend plus matching, Kafka lag, DB latency, Redis latency, rejection-rate, and fill-rate collectors.
  - Baseline done: `/api/ops/metrics` exposes in-process matching latency, rejection rate, fill rate, DB operation latency, Redis operation latency, and Kafka consumer lag counters. Remaining: production metrics backend/export.
- [x] Add request id / correlation id propagation through headers, MDC, outbox, Kafka, and external API clients.
- [ ] Add distributed tracing export, dashboards, and sampling policy.
- [x] Add request/security audit structured logging baseline.
- [x] Add structured core-event logging searchable by uid, orderId, clientOrderId, and symbol.
  - Baseline done: order lifecycle projection writes `CORE_EVENT eventType=ORDER_LIFECYCLE` log lines with stable `uid`, `orderId`, `clientOrderId`, `symbol`, `stage`, `status`, `reasonCode`, and `eventTs` fields.
- [ ] Add alerts for matching halt, Kafka lag, DLQ buildup, reconciliation failure, external API error rate, and unbalanced assets.

## P2 Incremental Evolution

- [ ] Admin console: market config, risk parameters, manual suspension, DLQ replay, reconciliation reports.
  - Task specs ready: [P2 evolution tasks](../tasks/p2/README.md) split admin market config, risk parameters, DLQ replay, and reconciliation report screens into implementable lanes.
- [ ] Reporting: user asset reports, trade reports, fee reports, operations and finance daily reports.
  - Task specs ready: [P2 evolution tasks](../tasks/p2/README.md) split user asset, trade, fee, and operations/finance daily report exports into implementable lanes.
- [ ] Load testing tools: order-entry TPS, matching TPS, market-data fanout, Polymarket sync pressure.
  - Task specs ready: [P2 evolution tasks](../tasks/p2/README.md) split order-entry TPS, matching TPS, and market-data fanout load tests into implementable lanes; Polymarket sync pressure remains part of the broader load-test roadmap.
- [ ] Gradual rollout and rollback: feature flags, canary deployment, schema backward compatibility.
  - Task spec ready: [feature flag, canary, and rollback](../tasks/p2/12-feature-flag-canary-rollback.md).
- [ ] Compliance hooks: KYC/AML integration, sanctions screening, trade surveillance, suspicious-activity reports.
  - Task specs ready: [KYC/AML/sanctions](../tasks/p2/13-kyc-aml-sanctions-integration.md) and [trade surveillance/SAR](../tasks/p2/14-trade-surveillance-sar.md).

## Suggested Near-Term Order

1. [x] Close the core-v1 freeze checklist.
2. [x] Run the core-v1 smoke runbook.
3. [x] Fix only release-blocking gaps found by tests/checklists.
4. [ ] Tag or hand off the bounded core-v1 baseline.
5. [x] Re-plan post-v1 work separately instead of adding it to this freeze.
