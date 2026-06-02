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
- Quote DTOs: `MarketMakerQuoteCommand`, `MarketMakerQuoteDecision`, `MarketMakerQuoteLifecycleReport`, `MarketMakerQuoteState`.
- Durable profile store: `MarketMakerProfileStore`, `JpaMarketMakerProfileStore`, `MarketMakerProfileRecord`, `MarketMakerRiskLimitRecord`.
- Durable quote state store: `MarketMakerQuoteStateStore`, `JpaMarketMakerQuoteStateStore`, `MarketMakerQuoteStateRecord`.
- Hedge audit store: `HedgeDecisionAuditStore`, `JpaHedgeDecisionAuditStore`, `HedgeDecisionAuditRecord`, `HedgeDecisionAuditRecordEntity`.
- Hedge fill store: `HedgeFillStore`, `JpaHedgeFillStore`, `HedgeFillRecord`, `HedgeFillRecordEntity`.
- Profile service: `MarketMakerProfileService`.
- Admin API: `interfaces.web.controller.MarketMakerController`, request DTOs `MarketMakerProfileRequest`, `MarketMakerRiskLimitRequest`.
- Quote API frequency limit: `MarketMakerQuoteRateLimiter` protects `POST /api/market-maker/quotes` before quote replacement side effects; config keys are `market-maker.api.quote-rate-limit.enabled`, `quotes-per-minute`, `max-tracked-keys`, and `client-ip-header`.
- Hedge execution API frequency limit: `MarketMakerHedgeExecutionRateLimiter` protects manual hedge execution endpoints before external venue routing; config keys are `market-maker.api.hedge-execution-rate-limit.enabled`, `executions-per-minute`, `max-tracked-keys`, and `client-ip-header`.
- Hedge fill/reconciliation admin queries: `/api/market-maker/profiles/{marketMakerId}/hedge-fills`, `/api/market-maker/hedge-fills/venue-orders/{venueOrderId}`, `/api/market-maker/hedge-fills/ref/{refId}`, `/api/market-maker/profiles/{marketMakerId}/hedge-reconciliation`, `/api/market-maker/hedge-idempotency/unresolved`, `POST /api/market-maker/hedge-idempotency/reconcile`.
- Hedge fill/reconciliation/idempotency operator views enforce bounded query limits of 1..500.
- Venue fill callback ingestion: `POST /api/market-maker/hedge-fills/venue-callback`, request DTO `HedgeVenueFillCallbackRequest`.
- Venue fill callback HMAC verification: `HedgeVenueCallbackVerifier` uses `X-Hedge-Venue-Timestamp`, `X-Hedge-Venue-Signature`, and `market-maker.hedge-callback.*`; default disabled for dev.
- Manual hedge execution admin commands: `POST /api/market-maker/profiles/{marketMakerId}/hedge-execution`, `POST /api/market-maker/hedge-execution/enabled`.
- Manual hedge execution validates bounded safe `refPrefix` values before building external venue ref ids.
- Scheduled hedge execution: `MarketMakerHedgeExecutionScheduler`, default disabled by `market-maker.hedge-execution.enabled=false`.
- Scheduled hedge execution can use durable worker locking through `market-maker.hedge-execution.lock-enabled=true`, `HedgeExecutionLockStore`, `JpaHedgeExecutionLockStore`, and `hedge_execution_locks`.
- Operator approval can be required through `market-maker.hedge-execution.approval-required=true`; manual APIs must pass `X-Operator-Approval`, while scheduler uses `scheduled-approval-token`.
- Exposure aggregation: `MarketMakerExposureService`.
- Quote validation and placement lifecycle: `MarketMakerQuoteService`, `MarketMakerQuoteLifecycleService`, `MarketMakerQuoteOrderGateway`, `UseCaseMarketMakerQuoteOrderGateway`; accepted or rejected quotes first clear prior open `mmq:{marketMakerId}:...` quote orders for the same uid/symbol before replacement placement, then persist latest active quote state for restart/operator lookup.
- Quote state versioning/reconciliation/repair: `MarketMakerQuoteState` keeps per-side bid/ask versions and replaced order ids; `MarketMakerQuoteReconciliationService` compares active quote state against open orders, reports missing tracked orders or untracked `mmq:` open orders, and can repair by canceling untracked quote orders or deactivating incomplete tracked state after canceling the remaining tracked quote order.
- Quote state operator APIs: `GET /api/market-maker/quotes/active`, `GET /api/market-maker/quotes/reconciliation`, `POST /api/market-maker/quotes/reconciliation/repair`, `GET /api/market-maker/profiles/{marketMakerId}/quotes`, `GET /api/market-maker/profiles/{marketMakerId}/quotes/{symbol}`.
- Inventory-aware hedge planning/execution: `MarketMakerHedgeStrategyService`, `MarketMakerHedgeExecutionService`, `HedgeStrategyDecision`, `HedgeExecutionReport`.
- Hedge execution entry points use `CommandTransactionBoundary` when Spring wires it, so profile lookup, exposure planning, venue routing, hedge decision audit, and outbox rows share one command boundary.
- Global execution halt: `risk-controls.market-maker-hedge-execution-halt` / `RISK_CONTROLS_MARKET_MAKER_HEDGE_EXECUTION_HALT`.
- Execution route cap policy: `risk-controls.market-maker-hedge-execution-policy.enabled`, `max-routed-orders-per-run`, and `max-routed-notional-per-run` cap venue routing per execution run. Enabled-profile batch execution shares one budget across profiles and emits `HEDGE_EXECUTION_POLICY_MAX_ORDERS` or `HEDGE_EXECUTION_POLICY_MAX_NOTIONAL` for skipped exposures.
- Quote repair scheduler: `MarketMakerQuoteRepairScheduler`, default disabled by `market-maker.quote-repair.enabled=false`; config keys are `MARKET_MAKER_QUOTE_REPAIR_ENABLED`, `MARKET_MAKER_QUOTE_REPAIR_FIXED_DELAY_MS`, and `MARKET_MAKER_QUOTE_REPAIR_LIMIT`.
- Hedge execution scheduler config: `MARKET_MAKER_HEDGE_EXECUTION_ENABLED`, `MARKET_MAKER_HEDGE_EXECUTION_FIXED_DELAY_MS`, `MARKET_MAKER_HEDGE_EXECUTION_REF_PREFIX`, `MARKET_MAKER_HEDGE_EXECUTION_LOCK_ENABLED`, `MARKET_MAKER_HEDGE_EXECUTION_LOCK_OWNER_ID`, `MARKET_MAKER_HEDGE_EXECUTION_LOCK_TTL_MS`, `MARKET_MAKER_HEDGE_EXECUTION_APPROVAL_REQUIRED`, `MARKET_MAKER_HEDGE_EXECUTION_APPROVAL_TOKEN`, `MARKET_MAKER_HEDGE_EXECUTION_SCHEDULED_APPROVAL_TOKEN`.
- Hedge decision/routing: `MarketMakerHedgingService`.
- Hedge fill recording: `MarketMakerHedgeFillService`; venue callback replay uses `venueOrderId + venueFillId` through `HedgeFillStore.findByVenueOrderIdAndVenueFillId(...)`.
- Hedge decision-vs-fill reconciliation and idempotency operator view: `MarketMakerHedgeReconciliationService`, `MarketMakerHedgeVenueIdempotencyService`, `HedgeReconciliationReport`, `HedgeReconciliationIssue`, `HedgeVenueIdempotencyReport`, `HedgeVenueIdempotencyIssue`; reconciliation now reports missing internal trade refs, missing ledger refs, and trade/ledger ref mismatches.
- Venue fill mapping/idempotency: `HedgeVenueFillMessage`, `HedgeVenueFillMapper`, `MarketMakerHedgeFillService.recordVenueFill(...)`, `HedgeFillStore`, `JpaHedgeFillStore`.
- Hedge venue contract: `domain.service.HedgeVenueAdapter`.
- Default safe adapter: `infra.hedging.RejectingHedgeVenueAdapter`.
- Real venue adapter transport: `infra.hedging.RealHedgeVenueAdapter`, `RealHedgeVenueOrderLookupAdapter`, `RealHedgeVenueHttpClient`, `RealHedgeVenueSigner`, and `SignedHedgeVenueRequest`; disabled mode safely rejects or returns empty lookup, enabled mode signs stable submit/lookup payloads, sends them through injected `OkHttpClient`, attaches submit idempotency headers, and maps venue accepted/rejected/retryable responses.
- Idempotency decorator baseline: `infra.hedging.IdempotentHedgeVenueAdapter` uses `HedgeOrderRequest.refId` with `HedgeVenueIdempotencyStore` / `JpaHedgeVenueIdempotencyStore` to claim before effectful venue submit, persist terminal results, prevent duplicate submits, reject payload conflicts, and block retries after pending or timeout-like uncertain outcomes. Operators can query unresolved pending/retryable records and trigger lookup reconciliation through `MarketMakerHedgeVenueIdempotencyService`.
- Venue outcome lookup contract: `HedgeVenueOrderLookupAdapter` with safe default `NoopHedgeVenueOrderLookupAdapter`; real venue adapters should implement lookup by `refId` and return terminal `HedgeOrderResult` for uncertain submit reconciliation.
- Retry/backoff/throttle decorator baseline: `infra.hedging.RetryingHedgeVenueAdapter`, `RetryBackoff`, `Sleeper`, `ThrottlingHedgeVenueAdapter`; `HedgeOrderResult.retryable` separates temporary venue errors from final rejections.
- Audit events: `HedgeDecisionRecorded`, `MarketMakerQuoteDecisionRecorded`.
- Tests: `MarketMakerHedgingServiceTest`, `MarketMakerQuoteServiceTest`, `MarketMakerQuoteLifecycleServiceTest`, `MarketMakerQuoteReconciliationServiceTest`, `MarketMakerQuoteStateRecordTest`, `UseCaseMarketMakerQuoteOrderGatewayTest`, `MarketMakerProfileServiceTest`, `MarketMakerHedgeFillServiceTest`, `HedgeVenueCallbackVerifierTest`, `MarketMakerHedgeReconciliationServiceTest`, `MarketMakerHedgeVenueIdempotencyServiceTest`, `MarketMakerHedgeStrategyServiceTest`, `MarketMakerHedgeExecutionServiceTest`, `MarketMakerQuoteRateLimiterTest`, `MarketMakerHedgeExecutionRateLimiterTest`, `RealHedgeVenueAdapterTest`, `IdempotentHedgeVenueAdapterTest`, `RetryingHedgeVenueAdapterTest`, `ThrottlingHedgeVenueAdapterTest`, `ApiAuthenticationInterceptorTest`.
- Migrations include hedge venue idempotency records in `V8__hedge_venue_idempotency_records.sql` and hedge trade/ledger refs in `V18__hedge_trade_ledger_refs.sql`.

## First Implementation Slice

1. [x] Define market-maker account/profile model and risk limits.
2. [x] Define hedge venue adapter interface with a fake/in-memory adapter for tests.
3. [x] Add quote/hedge command models and service boundaries.
4. [x] Add audit events for quote decisions, hedge decisions, and venue order id.
5. [x] Add tests covering exposure aggregation, quote kill switch, crossed quote, hedge kill switch, slippage rejection, and accepted venue routing.

- Migrations include market-maker quote states in `V16__market_maker_quote_states.sql` and per-side quote metadata in `V17__market_maker_quote_state_versions.sql`.

Remaining:
- Automated hedge reconciliation repair jobs.
