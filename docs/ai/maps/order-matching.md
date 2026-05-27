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
- Durable log/checkpoint migration: `V7__matching_replay_logs.sql`

Current behavior:
- Per-symbol operations are serialized by an in-process sequencer.
- LIMIT/MARKET, GTC/IOC/FOK, post-only rejection, self-match prevention, amend, cancel, top-of-book, and depth snapshot are covered.
- Snapshot export/restore preserves resting order FIFO and match sequence baseline.
- In-memory command log and replay API preserve snapshot checkpoint replay and match sequence continuation in tests.
- In-memory event log records emitted trade events with event offsets and their source command offset.
- Replay validation report compares command offset, event offset, match sequence, and book levels against expected snapshots.
- Spring wiring can use durable JPA command/event log adapters with per-symbol checkpoint rows and pessimistic offset locking.
- Matching snapshots have a durable JPA store; recovery orchestration still needs to wire latest snapshot + command replay into startup/worker takeover.
- Replay validation reports have a durable JPA store for recovery audit history.
- Production sequencer deployment and failover rules are documented in `docs/en/matching-sequencer-runbook.md`.

Remaining production TODO:
- Wire durable snapshots into startup/worker takeover recovery.
- Wire replay validation report persistence into startup/worker takeover recovery.
- Persist deterministic replay validation reports for production recovery.
- Add distributed sequencer lease / epoch fencing around worker ownership.
- Stronger cancel-replace atomicity and reconnect/session semantics.
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
