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
  -> position/account/ledger/market-data/lifecycle events
```

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
- Snapshot export/restore preserves resting order FIFO and match sequence baseline.
- In-memory command log and replay API preserve snapshot checkpoint replay and match sequence continuation in tests.
- In-memory event log records emitted trade events with event offsets and their source command offset.
- Replay validation report compares command offset, event offset, match sequence, and book levels against expected snapshots.
- Spring wiring can use durable JPA command/event log adapters with per-symbol checkpoint rows and pessimistic offset locking.
- Matching recovery orchestration can rebuild a symbol from latest snapshot plus later command log entries, then save the recovered snapshot.
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
- `InMemoryMatchingEngine.applyLoggedCommand(...)` propagates command owner/epoch to matching event logs during worker execution.
- Worker owner configuration is exposed through `matching-worker.*` / `MatchingWorkerProperties`; runbook documents `MATCHING_WORKER_ENABLED`, `MATCHING_WORKER_OWNER_ID`, `MATCHING_WORKER_SYMBOLS`, lease TTL, and renew interval.
- Matching command replay supports `CANCEL_REPLACE` with replacement order payload.
- Command/event log entries can persist sequencer `ownerId` and `ownerEpoch` for fencing audit.
- Production sequencer deployment and failover rules are documented in `docs/en/matching-sequencer-runbook.md`.

Remaining production TODO:
- Document the production deployment switch sequence and decide whether production worker routing can close after smoke verification.
- Stronger application/accounting cancel-replace atomicity and reconnect/session semantics.
- Keep this area first in the roadmap until replayable matching is complete.

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
