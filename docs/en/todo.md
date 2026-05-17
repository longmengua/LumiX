<!-- File purpose: English production-readiness checklist. Other languages are listed in the repository root README.md. -->
# Production TODO

This checklist focuses on the work needed to move the current MVP toward production. Priority can be adjusted by product stage, but P0/P1 items should be completed before real funds or real trading volume are introduced.

## P0 Required

### Trading and Matching

- Evolve the in-memory matching engine into a replayable matching core with command log, event log, snapshot, and offset checkpoint.
- Define deployment and failover rules for the per-symbol sequencer to prevent multiple instances from processing the same symbol concurrently.
- Add full order lifecycle events: created, accepted, rejected, partially filled, filled, canceled, expired.
- Add exchange-standard commands such as amend order, cancel replace, bulk cancel, and cancel on disconnect.
- Enforce tick size, lot size, min notional, price band, max order size, and max open orders.
- Make rejection semantics explicit for insufficient MARKET liquidity, unfilled IOC/FOK, POST_ONLY taking liquidity, and REDUCE_ONLY exceeding reducible position size.

### Accounting and Funds

- Build a complete double-entry ledger schema so every balance change is traceable, replayable, and reconcilable.
- Split order reserve, position margin, fee, rebate, realized PnL, funding, and liquidation loss into explicit accounting entries.
- Add immutable account calculations for frozen funds, released funds, available balance, total equity, maintenance margin, and risk ratio.
- Add end-of-day and near-real-time reconciliation jobs across accounts, positions, ledger, and event store.
- Add deposit/withdrawal state machines: pending, confirmed, failed, reversed, manual review.

### Risk

- Integrate mark price / index price oracles so liquidation and funding do not depend on trade price or arbitrary input.
- Add symbol risk tiers: max leverage, maintenance margin rate, initial margin rate, and stepped position limits.
- Complete pre-trade risk checks for balance, leverage, position, exposure, price deviation, frequency, and client order id deduplication.
- Complete the liquidation engine: scan, trigger, execute, insurance fund, ADL, and audit events.
- Add global risk switches: reduce-only mode, order-entry halt, withdrawal halt, and per-symbol suspension.

### Reliability and Consistency

- Make outbox durable and add retry backoff, max retry count, DLQ replay, and manual compensation workflow.
- Define Kafka partition keys, retention, compaction, schema versions, and consumer-group strategy.
- Add timeout, retry, circuit breaker, rate limit, and idempotency key to every external API call.
- Define transaction boundaries for core writes; MySQL, Redis, and Kafka must not be assumed to be automatically consistent.
- Build disaster recovery from snapshot + event log for matching, orders, accounts, and positions.

### Security

- Add API authentication and authorization: JWT, API keys, scopes, roles, and admin isolation.
- Move private keys, CLOB secrets, and relayer keys out of YAML into environment variables or a secret manager.
- Add session signer lifecycle controls: expiration, revocation, audit, and abnormal-use detection.
- Add rate limit, IP allowlist, and audit logs for trading, funds, and admin APIs.
- Prevent sensitive fields from entering logs: private key, API secret, passphrase, signature, authorization header.

## P1 Strongly Recommended

### Market Data

- Add incremental order book streams with sequence number, checksum, and snapshot + delta reconstruction.
- Persist ticker, kline, and trade tape so market data survives service restarts.
- Deploy WebSocket/SSE gateway independently with horizontal scaling, subscription authorization, heartbeat, rate limiting, and disconnect recovery.
- Add market-maker / liquidity-provider APIs and rate-limit policies.

### Polymarket Integration

- Build a Polymarket order state machine that tracks local order, CLOB order, trade, and settlement lifecycle.
- Version Gamma/CLOB response schemas to reduce breakage when remote fields change.
- Make CLOB place, cancel, sync, and reconcile commands idempotent.
- Deploy the user WebSocket service independently with reconnect, checkpoint, event deduplication, persistence, and replay.
- Add cache and expiry policy for allowance / approval checks to avoid overloading RPC endpoints.

### Database and Storage

- Add production indexes for orders, positions, ledger, events, and prediction orders.
- Document Redis key schema and add TTL, namespace, versioning, and migration strategy.
- Use Flyway as the single production schema manager; do not rely on Hibernate `ddl-auto=update`.
- Add archive strategy for historical orders, trades, ledger entries, Kafka events, and audit logs.

### Observability

- Add metrics for order latency, matching latency, Kafka lag, DB latency, Redis latency, rejection rate, and fill rate.
- Add tracing with request id / correlation id across API, UseCase, Kafka, and external APIs.
- Add structured logging so core events can be searched by uid, orderId, clientOrderId, and symbol.
- Add alerts for matching halt, Kafka lag, DLQ buildup, reconciliation failure, external API error rate, and unbalanced assets.

## P2 Incremental Evolution

- Admin console: market config, risk parameters, manual suspension, DLQ replay, reconciliation reports.
- Reporting: user asset reports, trade reports, fee reports, operations and finance daily reports.
- Load testing tools: order-entry TPS, matching TPS, market-data fanout, Polymarket sync pressure.
- Gradual rollout and rollback: feature flags, canary deployment, schema backward compatibility.
- Compliance hooks: KYC/AML integration, sanctions screening, trade surveillance, suspicious-activity reports.

## Suggested Near-Term Order

1. Add order lifecycle events and durable order/ledger/event schemas.
2. Add command log, snapshot, and replay to the matching engine.
3. Integrate mark price / index price and complete production-grade pre-trade risk and liquidation.
4. Build reconciliation jobs and an observability baseline.
5. Split WebSocket gateway, Polymarket WS worker, and matching worker.
