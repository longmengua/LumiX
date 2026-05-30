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
- Hedge fill/reconciliation/idempotency operator views enforce bounded query limits of 1..500.
- Venue fill callback ingestion: `POST /api/market-maker/hedge-fills/venue-callback`, request DTO `HedgeVenueFillCallbackRequest`.
- Venue fill callback HMAC verification: `HedgeVenueCallbackVerifier` uses `X-Hedge-Venue-Timestamp`, `X-Hedge-Venue-Signature`, and `market-maker.hedge-callback.*`; default disabled for dev.
- Manual hedge execution admin commands: `POST /api/market-maker/profiles/{marketMakerId}/hedge-execution`, `POST /api/market-maker/hedge-execution/enabled`.
- Manual hedge execution validates bounded safe `refPrefix` values before building external venue ref ids.
- Scheduled hedge execution: `MarketMakerHedgeExecutionScheduler`, default disabled by `market-maker.hedge-execution.enabled=false`.
- Scheduled hedge execution can use durable worker locking through `market-maker.hedge-execution.lock-enabled=true`, `HedgeExecutionLockStore`, `JpaHedgeExecutionLockStore`, and `hedge_execution_locks`.
- Operator approval can be required through `market-maker.hedge-execution.approval-required=true`; manual APIs must pass `X-Operator-Approval`, while scheduler uses `scheduled-approval-token`.
- Exposure aggregation: `MarketMakerExposureService`.
- Quote validation: `MarketMakerQuoteService`.
- Inventory-aware hedge planning/execution: `MarketMakerHedgeStrategyService`, `MarketMakerHedgeExecutionService`, `HedgeStrategyDecision`, `HedgeExecutionReport`.
- Hedge execution entry points use `CommandTransactionBoundary` when Spring wires it, so profile lookup, exposure planning, venue routing, hedge decision audit, and outbox rows share one command boundary.
- Global execution halt: `risk-controls.market-maker-hedge-execution-halt` / `RISK_CONTROLS_MARKET_MAKER_HEDGE_EXECUTION_HALT`.
- Execution route cap policy: `risk-controls.market-maker-hedge-execution-policy.enabled`, `max-routed-orders-per-run`, and `max-routed-notional-per-run` cap venue routing per execution run. Enabled-profile batch execution shares one budget across profiles and emits `HEDGE_EXECUTION_POLICY_MAX_ORDERS` or `HEDGE_EXECUTION_POLICY_MAX_NOTIONAL` for skipped exposures.
- Scheduler config: `MARKET_MAKER_HEDGE_EXECUTION_ENABLED`, `MARKET_MAKER_HEDGE_EXECUTION_FIXED_DELAY_MS`, `MARKET_MAKER_HEDGE_EXECUTION_REF_PREFIX`, `MARKET_MAKER_HEDGE_EXECUTION_LOCK_ENABLED`, `MARKET_MAKER_HEDGE_EXECUTION_LOCK_OWNER_ID`, `MARKET_MAKER_HEDGE_EXECUTION_LOCK_TTL_MS`, `MARKET_MAKER_HEDGE_EXECUTION_APPROVAL_REQUIRED`, `MARKET_MAKER_HEDGE_EXECUTION_APPROVAL_TOKEN`, `MARKET_MAKER_HEDGE_EXECUTION_SCHEDULED_APPROVAL_TOKEN`.
- Hedge decision/routing: `MarketMakerHedgingService`.
- Hedge fill recording: `MarketMakerHedgeFillService`; venue callback replay uses `venueOrderId + venueFillId` through `HedgeFillStore.findByVenueOrderIdAndVenueFillId(...)`.
- Hedge decision-vs-fill reconciliation and idempotency operator view: `MarketMakerHedgeReconciliationService`, `MarketMakerHedgeVenueIdempotencyService`, `HedgeReconciliationReport`, `HedgeReconciliationIssue`, `HedgeVenueIdempotencyReport`, `HedgeVenueIdempotencyIssue`.
- Venue fill mapping/idempotency: `HedgeVenueFillMessage`, `HedgeVenueFillMapper`, `MarketMakerHedgeFillService.recordVenueFill(...)`, `HedgeFillStore`, `JpaHedgeFillStore`.
- Hedge venue contract: `domain.service.HedgeVenueAdapter`.
- Default safe adapter: `infra.hedging.RejectingHedgeVenueAdapter`.
- Idempotency decorator baseline: `infra.hedging.IdempotentHedgeVenueAdapter` uses `HedgeOrderRequest.refId` with `HedgeVenueIdempotencyStore` / `JpaHedgeVenueIdempotencyStore` to claim before effectful venue submit, persist terminal results, prevent duplicate submits, reject payload conflicts, and block retries after pending or timeout-like uncertain outcomes. Operators can query unresolved pending/retryable records through `MarketMakerHedgeVenueIdempotencyService`.
- Retry/backoff/throttle decorator baseline: `infra.hedging.RetryingHedgeVenueAdapter`, `RetryBackoff`, `Sleeper`, `ThrottlingHedgeVenueAdapter`; `HedgeOrderResult.retryable` separates temporary venue errors from final rejections.
- Audit events: `HedgeDecisionRecorded`, `MarketMakerQuoteDecisionRecorded`.
- Tests: `MarketMakerHedgingServiceTest`, `MarketMakerQuoteServiceTest`, `MarketMakerProfileServiceTest`, `MarketMakerHedgeFillServiceTest`, `HedgeVenueCallbackVerifierTest`, `MarketMakerHedgeReconciliationServiceTest`, `MarketMakerHedgeVenueIdempotencyServiceTest`, `MarketMakerHedgeStrategyServiceTest`, `MarketMakerHedgeExecutionServiceTest`, `IdempotentHedgeVenueAdapterTest`, `RetryingHedgeVenueAdapterTest`, `ThrottlingHedgeVenueAdapterTest`, `ApiAuthenticationInterceptorTest`.
- Migrations include hedge venue idempotency records in `V8__hedge_venue_idempotency_records.sql`.

## First Implementation Slice

1. [x] Define market-maker account/profile model and risk limits.
2. [x] Define hedge venue adapter interface with a fake/in-memory adapter for tests.
3. [x] Add quote/hedge command models and service boundaries.
4. [x] Add audit events for quote decisions, hedge decisions, and venue order id.
5. [x] Add tests covering exposure aggregation, quote kill switch, crossed quote, hedge kill switch, slippage rejection, and accepted venue routing.

Remaining:
- Quote lifecycle integration with actual order placement/cancel-replace.
- Real hedge venue adapter, venue lookup for uncertain outcomes, and trade/ledger reconciliation refs.
