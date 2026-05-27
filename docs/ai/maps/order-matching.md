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

Current behavior:
- Per-symbol operations are serialized by an in-process sequencer.
- LIMIT/MARKET, GTC/IOC/FOK, post-only rejection, self-match prevention, amend, cancel, top-of-book, and depth snapshot are covered.
- Snapshot export/restore preserves resting order FIFO and match sequence baseline.
- In-memory command log and replay API preserve snapshot checkpoint replay and match sequence continuation in tests.
- Production sequencer deployment and failover rules are documented in `docs/en/matching-sequencer-runbook.md`.

Remaining production TODO:
- Move command log/event log, snapshots, and offset checkpoints from in-memory baseline to durable production storage.
- Add deterministic replay validation reports for production recovery.
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
