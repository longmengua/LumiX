# Task: Market Maker Hedging

Status: `doing`

## Goal

Build the market-maker interface and hedging strategy baseline: quoting, inventory, risk limits, kill switch, hedge order routing, venue adapter interface, slippage controls, and hedge audit trail.

## Scope

- Market-maker profile/account model.
- Quote command and inventory read model.
- Risk limit and kill switch behavior.
- Hedge venue adapter interface.
- Hedging decision service with exposure aggregation.
- Hedge audit events and reconciliation refs.

## First Implementation Slice

1. [x] Define market-maker profile and risk limit model.
2. [x] Define hedge venue adapter contract and fake adapter for tests.
3. [x] Add exposure aggregation service.
4. [x] Add slippage-control rejection tests.
5. [x] Emit hedge decision and hedge order audit events.

## Progress

- `MarketMakerProfile` and `MarketMakerRiskLimit` define market-maker uid isolation, per-symbol limits, slippage limit, and kill switch.
- `MarketMakerExposureService` aggregates market-maker positions with oracle mark prices into signed exposure notional.
- `MarketMakerQuoteService` validates quote commands for profile enabled state, risk-limit presence, kill switch, positive prices/quantities, and crossed quotes.
- `HedgeVenueAdapter` defines the hedge venue contract.
- `RejectingHedgeVenueAdapter` is the default safe adapter and rejects hedges until a real venue adapter is configured.
- `MarketMakerHedgingService` enforces enabled profile, risk-limit presence, kill switch, max order notional, and max slippage before routing.
- `HedgeDecisionRecorded` records accepted/rejected decisions, venue order id, order notional, and ref id.
- `MarketMakerQuoteDecisionRecorded` records accepted/rejected quote commands.
- `MarketMakerProfileStore`, `JpaMarketMakerProfileStore`, and `V14__market_maker_profiles.sql` add durable profile/risk-limit storage.
- `MarketMakerProfileService` validates and loads profiles by market-maker id, uid, and enabled status.
- `MarketMakerController` exposes admin endpoints under `/api/market-maker/profiles` for save/query/enabled profile management.
- `MarketMakerController` also exposes hedge fill query endpoints by market-maker id, venue order id, and ref id under `/api/market-maker/**`.
- `/api/market-maker/**` is classified as an admin API and covered by authentication tests.
- `HedgeDecisionAuditStore`, `JpaHedgeDecisionAuditStore`, and `V15__hedge_decision_audits.sql` persist hedge decision audit trails by market-maker, symbol, ref id, accepted status, reason, and venue order id.
- `MarketMakerHedgingService` writes hedge audit records for both rejected and accepted decisions.
- `HedgeFillStore`, `JpaHedgeFillStore`, and `V16__hedge_fills.sql` persist external hedge fill audit records by market-maker, venue order, venue fill id, ref id, side, quantity, price, and fee.
- `MarketMakerHedgeFillService` validates and records hedge fills, with query helpers for market-maker, venue order, and ref id.
- `MarketMakerHedgeReconciliationService` compares accepted hedge decisions against venue fills and reports missing, underfilled, or overfilled hedge orders.
- `MarketMakerController` exposes hedge reconciliation by market-maker id under `/api/market-maker/profiles/{marketMakerId}/hedge-reconciliation`.
- `MarketMakerHedgeStrategyService` creates reduce-only hedge plans from inventory exposure, per-symbol long/short limits, max order notional, and slippage limits.
- `MarketMakerHedgeExecutionService` executes reduce-only hedge plans for one market-maker or all enabled market-makers, returning `HedgeExecutionReport`.
- `risk-controls.market-maker-hedge-execution-policy.enabled` plus `max-routed-orders-per-run` can cap venue order routing per execution run and records `HEDGE_EXECUTION_POLICY_MAX_ORDERS` decisions for skipped exposures.
- `MarketMakerController` exposes manual hedge execution commands under `/api/market-maker/profiles/{marketMakerId}/hedge-execution` and `/api/market-maker/hedge-execution/enabled`.
- `risk-controls.market-maker-hedge-execution-halt` blocks hedge execution globally and returns `HEDGE_EXECUTION_HALTED` strategy decisions without routing to venue.
- `MarketMakerHedgeExecutionScheduler` can periodically execute enabled market-maker hedge plans, but defaults to `market-maker.hedge-execution.enabled=false`.
- `HedgeOrderResult.retryable` and `RetryingHedgeVenueAdapter` define the baseline retry contract for temporary venue failures without retrying final rejections.
- `RetryBackoff`, `Sleeper`, and `ThrottlingHedgeVenueAdapter` define testable backoff/throttle decorators for future real venue adapters.
- `HedgeVenueFillMessage`, `HedgeVenueFillMapper`, and `MarketMakerHedgeFillService.recordVenueFill(...)` define the standard fill mapping path from venue callbacks into durable hedge fill records.
- `MarketMakerController` exposes venue fill callback ingestion at `/api/market-maker/hedge-fills/venue-callback`.
- Tests cover exposure aggregation, quote kill-switch rejection, crossed quote rejection, hedge kill-switch rejection, slippage rejection, and accepted venue routing.
- `MarketMakerQuoteReconciliationService.repairActiveQuotes(...)`, `MarketMakerQuoteRepairScheduler`, and `POST /api/market-maker/quotes/reconciliation/repair` add fail-closed quote repair: untracked quote orders are canceled, and incomplete tracked bid/ask state is deactivated after canceling the remaining tracked quote order.

## Remaining Work

- Add real hedge venue adapter with signing and production callback authentication/verification.
- Extend hedge reconciliation from decision-vs-fill checks into trade/ledger refs.
- Add automated hedge reconciliation repair jobs.

## Acceptance Criteria

- Market-maker actions are isolated from ordinary user flows where needed.
- Hedge decisions are auditable and tied to exposure/trade refs.
- Kill switch blocks quoting and hedging safely.

## Read First

- [../../ai/maps/market-maker-hedging.md](../../ai/maps/market-maker-hedging.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
