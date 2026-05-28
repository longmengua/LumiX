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
- Matching command replay supports `CANCEL_REPLACE` with replacement order payload.
- Command/event log entries can persist sequencer `ownerId` and `ownerEpoch` for fencing audit.
- Production sequencer deployment and failover rules are documented in `docs/en/matching-sequencer-runbook.md`.

Remaining production TODO:
- Wire lease write guard into the production worker command pipeline.
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
