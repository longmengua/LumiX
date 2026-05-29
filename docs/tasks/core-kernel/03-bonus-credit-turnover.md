# Task: Bonus Credit And Turnover

Status: `doing`

## Goal

Add bonus-credit / experience-fund accounting and turnover tracking so promotional funds and user trading volume can be audited separately from real cash.

## Scope

- Separate ledger accounts for real balance and bonus credit.
- Eligibility rules for bonus use.
- Consumption priority between real balance and bonus credit.
- Expiry, clawback, and manual adjustment.
- Turnover tracking by user, account, symbol, strategy, and market-maker dimensions.
- Reconciliation between turnover, trades, and ledger postings.

## First Implementation Slice

1. [x] Define bonus credit ledger account codes and posting reasons.
2. [x] Add turnover record/read model.
3. [x] Update trade accounting to emit turnover facts.
4. [x] Add tests for bonus consumption and turnover reconciliation.

## Progress

- `WalletLedgerService` now has explicit bonus-credit postings:
  `bonus_credit_grant`, `bonus_credit_consume`, `bonus_credit_expire`, and `bonus_credit_clawback`.
- Bonus credit uses `USER_BONUS_AVAILABLE` and does not change `Account.crossBalance`, so it cannot silently mix with real cash.
- `BonusCreditService` now keeps grant batches with remaining amount and expiry, consumes by expiry FIFO, and can expire due grants.
- `BonusCreditService` now reports per-user bonus-credit grant state and performs active-grant FIFO clawback with ledger entries.
- `MarginController` exposes `GET /api/margin/bonus-credit/report` and `POST /api/margin/bonus-credit/clawback`; these inherit the existing `/api/margin/**` funds security classification.
- `BonusCreditExpiryScheduler` provides a disabled-by-default scheduler entry via `bonus-credit.expiry-enabled`.
- `TurnoverService` records `TradeExecuted` facts into `TurnoverRecord` with uid, account, symbol, strategy, order, match, sequence, quantity, price, and notional dimensions.
- `TurnoverService` can summarize turnover by uid with optional symbol, strategy, market-maker, and match filters.
- `MarginController` exposes `GET /api/margin/turnover/summary` for operations and campaign reporting baselines.
- `OrderService` can optionally write turnover facts after idempotent trade accounting.
- `V11__turnover_records.sql` adds the durable turnover read model with indexes for uid, symbol, strategy, market-maker, and match lookups.
- `V12__bonus_credit_grants.sql` adds the durable bonus grant read model with uid/asset/status/expiry indexes.
- Tests cover bonus lifecycle separation from cash, clawback capping, turnover fact creation, and match-level turnover summary.

## Remaining Work

- Add bonus eligibility rules per product/symbol/order type before bonus consumption is allowed.
- Add first-class `strategyId` / `marketMakerId` fields to order entry instead of temporarily deriving strategy from `clientOrderId`.
- Add turnover reconciliation job that compares turnover records against trade tape and ledger refs.
- Expand bonus reporting into campaign/operator reports and add paged turnover drill-down APIs.

## Acceptance Criteria

- Bonus credit cannot be mixed silently with real cash.
- Expired or clawed-back bonus credit is auditable.
- Turnover totals can be compared against trade tape and ledger refs.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
