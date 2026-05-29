<!-- File purpose: English production-readiness checklist. Other languages are listed in the repository root README.md. -->
# Production TODO

This checklist focuses on the work needed to move the current MVP toward production. Core-v1 freeze is closed; the next core production lane is [post-v1 production hardening tasks](../tasks/post-v1/README.md).

Documentation categories: [Product Documentation](README.md) / [Technical Documentation](technical.md) / TODO Documentation

## Active Freeze Work

- [x] Close [core-v1-release-checklist.md](core-v1-release-checklist.md).
- [x] Run [core-v1-smoke-runbook.md](core-v1-smoke-runbook.md).
- [x] Fix only compile/test/checklist gaps discovered during freeze verification.
- [x] Defer web apps, Polymarket production worker split, broad reporting, compliance, and observability expansion until after core-v1 is tagged.

## P0 Required

### Core Exchange Kernel Priority Lane

- [ ] Finish the replayable matching core: durable command log, event log, snapshots, offset checkpoints, and deterministic replay validation.
- [ ] Complete production ADL: queue ranking, forced deleveraging execution, audit events, insurance-fund interaction, and operator controls.
  - Baseline done: deterministic ranking/planning, liquidation decision audit, operator halt/manual-review hooks, forced-execution service for position reduction, ledger postings, audit events, durable execution summary/idempotency records, queue-to-execution orchestration, operator claim/release guard, and partial-execution retry semantics.
- [ ] Add bonus-credit / experience-fund accounting with separate ledger accounts, eligibility rules, consumption priority, expiry, clawback, and reporting.
  - Baseline done: separate bonus ledger account, grant/consume/expiry/clawback postings, cash-balance isolation, grant-batch remaining tracking, and disabled-by-default expiry scanner.
- [ ] Add turnover tracking for user, account, symbol, strategy, and market-maker dimensions, with ledger/trade reconciliation.
  - Baseline done: durable turnover records emitted from processed trades; reconciliation job and first-class strategy/market-maker order fields remain.
- [ ] Harden ledger reconciliation into an auditable accounting book: immutable journals, trial balance, replay comparison, exception workflow, and finance reports.
  - Baseline done: trial-balance calculation, structured replay comparison, reconciliation issue workflow fields/admin APIs, and workflow audit events; daily reports remain.
- [ ] Build market-maker interfaces for quoting, inventory, risk limits, kill switch, and hedging order routing.
  - Baseline done: durable profile/risk-limit storage, admin profile APIs, exposure aggregation, quote command validation, kill switch, slippage control, hedge venue contract, safe rejecting adapter, quote/hedge decision audit events, durable hedge decision audit trails, and hedge fill audit persistence.
- [ ] Build market-maker hedging strategy baseline: exposure aggregation, hedge venue adapter interface, execution policy, slippage controls, and hedge audit trail.
  - Baseline done: exposure aggregation, hedge venue adapter interface, slippage controls, durable hedge decision audit trail, and fill audit persistence; execution policy and fill integration remain.

### Trading and Matching

- [ ] Evolve the in-memory matching engine into a replayable matching core with command log, event log, snapshot, and offset checkpoint.
- [x] Add an in-process per-symbol sequencer baseline so matching operations for the same symbol are serialized.
- [x] Define production deployment and failover rules for the per-symbol sequencer to prevent multiple instances from processing the same symbol concurrently.
- [x] Publish order lifecycle events for created, accepted, updated, rejected, canceled, expired, and filled states.
- [x] Persist and operationalize order lifecycle events with durable storage, schema versioning, replay, and query projections.
- [x] Add REST/WebSocket baselines for amend order, cancel replace, bulk cancel, and cancel on disconnect.
- [ ] Add durable command logs, stronger cancel-replace atomicity modes, and reconnect/session semantics for exchange-standard commands.
  - Baseline done: durable matching command/event logs, worker fencing, cancel-replace command replay, and cancel-on-disconnect connection resume semantics.
- [x] Enforce tick size, lot size, min notional, price band, max order size, and max open orders in pre-trade checks.
- [x] Make rejection semantics explicit for insufficient MARKET liquidity, unfilled IOC/FOK, POST_ONLY taking liquidity, and REDUCE_ONLY exceeding reducible position size.

### Accounting and Funds

- [x] Add a balanced wallet-ledger posting baseline so balance changes are traceable and reconcilable in the MVP.
- [x] Build the complete production double-entry ledger schema and replay path.
- [x] Split order reserve, position margin, fee, rebate, realized PnL, funding, liquidation shortfall, deposit, and withdrawal into explicit accounting entries.
- [ ] Harden accounting entries with production database constraints, audit retention, and replay validation.
- [x] Add `/api/margin/risk` for frozen funds, available balance, total equity, maintenance margin, and risk ratio snapshots.
- [x] Persist daily account risk snapshots and fully replace trade/ticker fallback marks with independent mark/index oracle inputs.
- [x] Add an all-account reconciliation baseline that scans the maintained account index plus open-position index and reports account, position margin, and ledger-balance issues.
- [x] Add persisted reconciliation reports, scheduling policy, alert routing, and event-store coverage.
- [x] Add a Redis-backed deposit/withdrawal state-machine baseline for pending, confirmed, failed, reversed, and manual review transfer states.
- [ ] Add chain/bank callbacks, manual-review workflow ownership, and transfer reconciliation projections.

### Risk

- [x] Integrate mark price / index price oracles so liquidation and funding do not depend on trade price or arbitrary input.
- [x] Add symbol risk baseline settings for max leverage, maintenance margin rate, max position notional, and max open orders.
- [x] Add full risk tiers with initial margin rate and stepped position limits.
- [x] Add pre-trade risk checks for balance, leverage, position, exposure, price deviation, and client order id deduplication.
- [ ] Add production frequency limits and broader abuse controls to pre-trade risk checks.
- [x] Add a liquidation MVP with trigger, close, insurance fund, ADL, and audit event coverage.
- [ ] Add production liquidation scanning, execution routing, and operational controls.
- [x] Add global risk switches for reduce-only mode, order-entry halt, withdrawal halt, and per-symbol suspension.

### Reliability and Consistency

- [x] Add outbox retry backoff, max retry count, DLQ replay, and manual compensation workflow baseline.
- [x] Move outbox to production durable storage and add manual compensation runbooks.
- [x] Document Kafka topic partition keys, retention, compaction, schema versions, and consumer-group strategy.
- [x] Add shared HTTP timeout, retry, circuit breaker, and rate-limit baseline for external API calls.
- [ ] Verify timeout, retry, circuit breaker, rate limit, and idempotency coverage for every external API call.
  - Baseline done: external API inventory, durable hedge venue submit idempotency envelope using `refId`, CLOB place local idempotency using `clientRequestId`, CLOB cancel local replay for already-recorded cancel/uncertain statuses, CLOB sync/reconcile no-op local replay for unchanged payloads, and approval read TTL cache coverage; durable CLOB command identity, cancel uncertainty resolution, RPC transaction tracking, and callback effectful idempotency remain.
- [ ] Define transaction boundaries for core writes; MySQL, Redis, and Kafka must not be assumed to be automatically consistent.
- [x] Add MVP snapshot + event replay recovery entry points.
- [ ] Build production disaster recovery for matching, orders, accounts, and positions.

### Security

- [ ] Add session signer lifecycle controls: expiration, revocation, audit, and abnormal-use detection.

## P1 Strongly Recommended

### Market Data

- [x] Add REST/SSE depth delta with monotonic version and CRC32 checksum for snapshot + delta validation.
- [x] Add durable sequence checkpoints and reconnect backfill for incremental order book streams.
  - Baseline done: durable depth-delta sequence/checksum checkpoints, duplicate/out-of-order checkpoint ignoring, startup recovery of latest depth sequence, durable depth delta records, `GET /api/market-data/{symbol}/depth-deltas?afterVersion=...` reconnect backfill, durable trade tape records, durable ticker latest-state records, durable 1m kline records, and disabled-by-default DB retention windows.
- [x] Define retention/archive policy for high-volume market-data depth, trade, and kline history.
  - Baseline done: DB retention job purges depth delta, trade tape, and 1m kline history by independent windows; production archive export/storage remains a broader ops task.
- [ ] Deploy WebSocket/SSE gateway independently with horizontal scaling, subscription authorization, heartbeat, rate limiting, and disconnect recovery.
- [ ] Add market-maker / liquidity-provider API hardening and rate-limit policies after the P0 market-maker interface baseline is complete.

### Polymarket Integration

- [ ] Build a Polymarket order state machine that tracks local order, CLOB order, trade, and settlement lifecycle.
- [ ] Version Gamma/CLOB response schemas to reduce breakage when remote fields change.
- [ ] Make CLOB place, cancel, sync, and reconcile commands idempotent.
  - Baseline done: place can use `clientRequestId`; cancel locally replays already-recorded cancel/uncertain statuses; sync/reconcile skip unchanged local writes. Remaining: durable command identity, remote lookup/reconcile to resolve uncertain cancel, and full state-machine transitions.
  - Baseline done: place supports `clientRequestId` duplicate replay, payload conflict rejection, and uncertain local-order retry blocking.
- [ ] Deploy the user WebSocket service independently with reconnect, checkpoint, event deduplication, persistence, and replay.
- [ ] Add cache and expiry policy for allowance / approval checks to avoid overloading RPC endpoints.

### Database and Storage

- [ ] Add production indexes for orders, positions, ledger, events, and prediction orders.
- [x] Document Redis key schema, namespace prefix, versioning, and migration strategy.
- [ ] Add final TTL/archive rules for Redis hot-state keys.
- [x] Use Flyway as the single production schema manager; do not rely on Hibernate `ddl-auto=update`.
- [ ] Add archive strategy for historical orders, trades, ledger entries, Kafka events, and audit logs.

### Observability

- [x] Add `/api/ops/metrics` baseline for order status, order latency, cancel count, and trade-event count.
- [ ] Add metrics backend plus matching, Kafka lag, DB latency, Redis latency, rejection-rate, and fill-rate collectors.
- [x] Add request id / correlation id propagation through headers, MDC, outbox, Kafka, and external API clients.
- [ ] Add distributed tracing export, dashboards, and sampling policy.
- [x] Add request/security audit structured logging baseline.
- [ ] Add structured core-event logging searchable by uid, orderId, clientOrderId, and symbol.
- [ ] Add alerts for matching halt, Kafka lag, DLQ buildup, reconciliation failure, external API error rate, and unbalanced assets.

## P2 Incremental Evolution

- [ ] Admin console: market config, risk parameters, manual suspension, DLQ replay, reconciliation reports.
- [ ] Reporting: user asset reports, trade reports, fee reports, operations and finance daily reports.
- [ ] Load testing tools: order-entry TPS, matching TPS, market-data fanout, Polymarket sync pressure.
- [ ] Gradual rollout and rollback: feature flags, canary deployment, schema backward compatibility.
- [ ] Compliance hooks: KYC/AML integration, sanctions screening, trade surveillance, suspicious-activity reports.

## Suggested Near-Term Order

1. [x] Close the core-v1 freeze checklist.
2. [x] Run the core-v1 smoke runbook.
3. [x] Fix only release-blocking gaps found by tests/checklists.
4. [ ] Tag or hand off the bounded core-v1 baseline.
5. [x] Re-plan post-v1 work separately instead of adding it to this freeze.
