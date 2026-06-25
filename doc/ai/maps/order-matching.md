# Order And Matching Map

## Order Placement

- API: `interfaces.web.controller.OrderController`
- Request DTO: `interfaces.web.dto.PlaceOrderRequest`
- Command: `application.command.PlaceOrderCommand`
- Use case: `application.usecase.PlaceOrderUseCase`
- Pre-trade checks and reserve: `application.service.RiskService`
- Matching/accounting orchestration: `application.service.OrderService`
- Tests: `application.service.OrderAccountingIntegrationTest`

Flow:

```text
OrderController
  -> PlaceOrderUseCase
  -> RiskService.preCheckAndReserve
  -> OrderService.processOrder
  -> MatchingEngine.submit
  -> product-type accounting
     - PERPETUAL: position margin, PnL, funding/liquidation-compatible position state
     - SPOT: base/quote asset settlement without Position creation
  -> symbol config gates
     - tradingEnabled / visible / reduceOnly / productType / tick / lot / notional / risk tiers
  -> account/ledger/market-data/lifecycle events
```

## Product Types

- Product discriminator: `domain.model.enums.ProductType`.
- Symbol config: `SymbolConfig.productType`, defaulting to `PERPETUAL` for backward compatibility.
- Default markets:
  - `BTCUSDT` and `BTCUSDT-PERP`: perpetual contract path.
  - `BTCUSDT-SPOT`: spot asset-settlement path.
- `Symbol.code()` can now preserve explicit internal symbols such as `BTCUSDT-SPOT`, so spot and perpetual books do not collide when base/quote are the same.

## Matching Core

- Contract: `domain.service.MatchingEngine`
- Current adapter: `infra.matching.InMemoryMatchingEngine`
- Book: `domain.service.OrderBook`
- Snapshots:
  - Read model: `domain.service.OrderBookSnapshot`
  - Recovery baseline: `domain.model.dto.MatchingEngineSnapshot`
- Main model: `domain.model.entity.Order`
- Enums: `OrderSide`, `OrderType`, `TimeInForce`
- Events: `domain.event.TradeExecuted`
- Tests: `infra.matching.InMemoryMatchingEngineTest`
- Durable log adapters: `JpaMatchingCommandLog`, `JpaMatchingEventLog`
- Durable snapshot store: `MatchingSnapshotStore`, `JpaMatchingSnapshotStore`
- Durable replay report store: `MatchingReplayValidationReportStore`, `JpaMatchingReplayValidationReportStore`
- Recovery orchestration: `application.service.MatchingRecoveryService`
- Sequencer lease: `MatchingSequencerLeaseStore`, `JpaMatchingSequencerLeaseStore`, `MatchingSequencerLeaseService`
- Durable log/checkpoint migration: `V7__matching_replay_logs.sql`
- Durable lease migration: `V8__matching_sequencer_leases.sql`
- Cancel-replace migration: `V9__matching_cancel_replace_commands.sql`
- Owner epoch log migration: `V10__matching_owner_epoch_logs.sql`

Current behavior:
- Per-symbol operations are serialized by an in-process sequencer.
- LIMIT/MARKET, GTC/IOC/FOK, post-only rejection, self-match prevention, amend, cancel, top-of-book, and depth snapshot are covered.
- Spot and perpetual products share matching semantics but split risk reserve and settlement accounting by `ProductType`.
- Order intake always resolves `SymbolConfig` before reserve/matching so symbol visibility and trading enablement stay consistent with admin config.
- Snapshot export/restore preserves resting order FIFO and match sequence baseline.
- In-memory command log and replay API preserve snapshot checkpoint replay and match sequence continuation in tests.
- In-memory event log records emitted trade events with event offsets and their source command offset.
- Replay validation report compares command offset, event offset, match sequence, and book levels against expected snapshots.
- Spring wiring can use durable JPA command/event log adapters with per-symbol checkpoint rows and pessimistic offset locking.
- Matching recovery orchestration can rebuild a nonblank symbol from latest snapshot plus later command log entries, then save the recovered snapshot.
- Replay validation reports have a durable JPA store for recovery audit history.
- Sequencer lease service manages per-symbol owner acquire/renew/release and increments epoch on takeover.
- Sequencer lease service exposes `requireWritable(...)` to reject missing lease, wrong owner, stale epoch, and expired lease before command writes.
- `MatchingWorkerCommandRouter` is the production-worker-facing append boundary; it calls `requireWritable(...)` before command/event append and writes owner id / epoch into matching logs.
- `MatchingWorkerExecutionService` provides worker submit, cancel, amend, and cancel-replace paths: append a lease-fenced command first, then execute the already-logged command through `MatchingEngine.applyLoggedCommand(...)` without duplicate command append.
- `MatchingWorkerLifecycleService` starts configured worker symbols by acquiring a lease, running recovery, validating replay, retaining owner/epoch readiness context, and renewing leases with command/event checkpoints.
- `MatchingWorkerLeaseRenewalScheduler` is the scheduled renewal entry point; it is gated by `matching-worker.enabled` and relies on the lifecycle service to remove readiness when renewal fails.
- `RecoveryController` exposes matching worker owner/readiness context under `/api/recovery/matching-worker/contexts` for routing and operations inspection.
- `OrderService`, `CancelOrderUseCase`, and `AmendOrderUseCase` route submit/cancel/amend through `MatchingWorkerExecutionService` when a ready worker owner context exists for the symbol, otherwise they preserve the legacy in-process path.
- `CancelReplaceOrderUseCase` remains an accounting-safe cancel + replacement-submit orchestration; when worker context is ready, both legs are fenced worker commands.
- `matching-worker.fence-legacy-routing` rejects fallback to legacy in-process routing for configured symbols when worker readiness is missing.
- `MatchingWorkerStartupListener` starts configured symbols on `ApplicationReadyEvent` when `matching-worker.enabled=true`.
- `MatchingBookRecoveryStartupListener` rebuilds local in-memory books on `ApplicationReadyEvent` when REST-mode matching is active (`matching-worker.enabled=false`), first via durable recovery and then, if needed, with persisted open-order fallback from `OrderRepository.openOrders()`.
- `RedisOrderRepository` maintains a global order id index for open-order fallback recovery; legacy Redis data without that index can be scanned from per-user lists during MVP startup compatibility.
- `InMemoryMatchingEngine.applyLoggedCommand(...)` propagates command owner/epoch to matching event logs during worker execution.
- Worker owner configuration is exposed through `matching-worker.*` / `MatchingWorkerProperties`; runbook documents `MATCHING_WORKER_ENABLED`, `MATCHING_WORKER_OWNER_ID`, `MATCHING_WORKER_SYMBOLS`, lease TTL, and renew interval.
- Matching command replay supports `CANCEL_REPLACE` with replacement order payload.
- Command/event log entries can persist sequencer `ownerId` and `ownerEpoch` for fencing audit.
- Cancel-on-disconnect supports connection resume by moving a registration from `resumeConnectionId` to the new WebSocket session id, preventing late old-close events from canceling orders after reconnect.
- Matching restore drills assert that recovered open orders from latest snapshot plus later command log entries are present after recovery.
- Replay validation has multi-symbol interleaved command-offset coverage, so per-symbol offsets remain independent when command writes are interleaved.
- Production sequencer deployment and failover rules are documented in `doc/reliability/matching-sequencer-runbook.md`.
- Disaster recovery worker takeover, authenticated command reconnect/session replay semantics, restore smoke commands, and account/position consistency validation are documented in `doc/runbooks/disaster-recovery-runbook.md`.

Remaining production TODO:
- Multi-process operational hardening beyond the current in-process engine worker baseline.

## Order Management

- Cancel: `application.usecase.CancelOrderUseCase`
- Amend: `application.usecase.AmendOrderUseCase`
- Cancel-replace: `application.usecase.CancelReplaceOrderUseCase`
- Cancel-on-disconnect: `application.service.CancelOnDisconnectService`
- Lifecycle projection: `application.service.OrderLifecycleProjectionService`
- Persistence models: `OrderLifecycleEventRecord`, `OrderLifecycleProjection`
- Migration: `V2__order_lifecycle_projection.sql`

Check when changing:
- Reserve reconciliation in `RiskService`.
- Market data refresh after book mutation.
- Lifecycle event stages and projection rebuild behavior.
