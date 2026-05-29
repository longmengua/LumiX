# Market Maker And Hedging Map

This map is part of the current core-kernel priority lane. It should be read when working on market-maker, liquidity-provider, or hedging tasks.

## Priority Scope

- Market-maker quoting interface.
- Inventory and exposure tracking.
- Per-symbol and global risk limits.
- Kill switch and reduce-only behavior for market-maker accounts.
- Hedge order routing interface.
- Hedge venue adapter contract.
- Hedging strategy baseline with execution policy and slippage controls.
- Hedge audit trail and reconciliation against trade/ledger state.

## Likely Code Areas

- Order entry and lifecycle: `application.usecase`, `application.service.OrderService`
- Risk limits: `application.service.RiskService`
- Market data: `application.service.MarketDataService`
- Ledger/reconciliation: `WalletLedgerService`, `ReconciliationService`
- Future market-maker package should stay behind application/domain contracts before adding infra adapters.
- Market-maker DTOs: `MarketMakerProfile`, `MarketMakerRiskLimit`, `MarketMakerExposure`, `HedgeOrderRequest`, `HedgeOrderResult`, `HedgeDecision`.
- Quote DTOs: `MarketMakerQuoteCommand`, `MarketMakerQuoteDecision`.
- Durable profile store: `MarketMakerProfileStore`, `JpaMarketMakerProfileStore`, `MarketMakerProfileRecord`, `MarketMakerRiskLimitRecord`.
- Hedge audit store: `HedgeDecisionAuditStore`, `JpaHedgeDecisionAuditStore`, `HedgeDecisionAuditRecord`, `HedgeDecisionAuditRecordEntity`.
- Hedge fill store: `HedgeFillStore`, `JpaHedgeFillStore`, `HedgeFillRecord`, `HedgeFillRecordEntity`.
- Profile service: `MarketMakerProfileService`.
- Admin API: `interfaces.web.controller.MarketMakerController`, request DTOs `MarketMakerProfileRequest`, `MarketMakerRiskLimitRequest`.
- Hedge fill/reconciliation admin queries: `/api/market-maker/profiles/{marketMakerId}/hedge-fills`, `/api/market-maker/hedge-fills/venue-orders/{venueOrderId}`, `/api/market-maker/hedge-fills/ref/{refId}`, `/api/market-maker/profiles/{marketMakerId}/hedge-reconciliation`, `/api/market-maker/hedge-idempotency/unresolved`.
- Venue fill callback ingestion: `POST /api/market-maker/hedge-fills/venue-callback`, request DTO `HedgeVenueFillCallbackRequest`.
- Manual hedge execution admin commands: `POST /api/market-maker/profiles/{marketMakerId}/hedge-execution`, `POST /api/market-maker/hedge-execution/enabled`.
- Scheduled hedge execution: `MarketMakerHedgeExecutionScheduler`, default disabled by `market-maker.hedge-execution.enabled=false`.
- Exposure aggregation: `MarketMakerExposureService`.
- Quote validation: `MarketMakerQuoteService`.
- Inventory-aware hedge planning/execution: `MarketMakerHedgeStrategyService`, `MarketMakerHedgeExecutionService`, `HedgeStrategyDecision`, `HedgeExecutionReport`.
- Hedge execution entry points use `CommandTransactionBoundary` when Spring wires it, so profile lookup, exposure planning, venue routing, hedge decision audit, and outbox rows share one command boundary.
- Global execution halt: `risk-controls.market-maker-hedge-execution-halt` / `RISK_CONTROLS_MARKET_MAKER_HEDGE_EXECUTION_HALT`.
- Scheduler config: `MARKET_MAKER_HEDGE_EXECUTION_ENABLED`, `MARKET_MAKER_HEDGE_EXECUTION_FIXED_DELAY_MS`, `MARKET_MAKER_HEDGE_EXECUTION_REF_PREFIX`.
- Hedge decision/routing: `MarketMakerHedgingService`.
- Hedge fill recording: `MarketMakerHedgeFillService`.
- Hedge decision-vs-fill reconciliation and idempotency operator view: `MarketMakerHedgeReconciliationService`, `MarketMakerHedgeVenueIdempotencyService`, `HedgeReconciliationReport`, `HedgeReconciliationIssue`, `HedgeVenueIdempotencyReport`, `HedgeVenueIdempotencyIssue`.
- Venue fill mapping: `HedgeVenueFillMessage`, `HedgeVenueFillMapper`, `MarketMakerHedgeFillService.recordVenueFill(...)`.
- Hedge venue contract: `domain.service.HedgeVenueAdapter`.
- Default safe adapter: `infra.hedging.RejectingHedgeVenueAdapter`.
- Idempotency decorator baseline: `infra.hedging.IdempotentHedgeVenueAdapter` uses `HedgeOrderRequest.refId` with `HedgeVenueIdempotencyStore` / `JpaHedgeVenueIdempotencyStore` to claim before effectful venue submit, persist terminal results, prevent duplicate submits, reject payload conflicts, and block retries after pending or timeout-like uncertain outcomes. Operators can query unresolved pending/retryable records through `MarketMakerHedgeVenueIdempotencyService`.
- Retry/backoff/throttle decorator baseline: `infra.hedging.RetryingHedgeVenueAdapter`, `RetryBackoff`, `Sleeper`, `ThrottlingHedgeVenueAdapter`; `HedgeOrderResult.retryable` separates temporary venue errors from final rejections.
- Audit events: `HedgeDecisionRecorded`, `MarketMakerQuoteDecisionRecorded`.
- Tests: `MarketMakerHedgingServiceTest`, `MarketMakerQuoteServiceTest`, `MarketMakerProfileServiceTest`, `MarketMakerHedgeFillServiceTest`, `MarketMakerHedgeReconciliationServiceTest`, `MarketMakerHedgeVenueIdempotencyServiceTest`, `MarketMakerHedgeStrategyServiceTest`, `MarketMakerHedgeExecutionServiceTest`, `IdempotentHedgeVenueAdapterTest`, `RetryingHedgeVenueAdapterTest`, `ThrottlingHedgeVenueAdapterTest`, `ApiAuthenticationInterceptorTest`.
- Migrations include hedge venue idempotency records in `V8__hedge_venue_idempotency_records.sql`.

## First Implementation Slice

1. [x] Define market-maker account/profile model and risk limits.
2. [x] Define hedge venue adapter interface with a fake/in-memory adapter for tests.
3. [x] Add quote/hedge command models and service boundaries.
4. [x] Add audit events for quote decisions, hedge decisions, and venue order id.
5. [x] Add tests covering exposure aggregation, quote kill switch, crossed quote, hedge kill switch, slippage rejection, and accepted venue routing.

Remaining:
- Quote lifecycle integration with actual order placement/cancel-replace.
- Profile/fill API authorization and validation hardening.
- Real hedge venue adapter, venue callback ingestion endpoint, and trade/ledger reconciliation refs.
- Production execution policy, global limits, scheduler/worker locking, and operator approval flow.
