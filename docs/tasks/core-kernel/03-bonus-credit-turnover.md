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
- `BonusCreditService` now has a configurable consume eligibility gate for allowed/blocked symbols, allowed order types, and allowed expense accounts under `bonus-credit.eligibility`.
- `BonusCreditService` now reports per-user and per-campaign bonus-credit grant state and performs user or campaign active-grant FIFO clawback with ledger entries.
- `MarginController` exposes `GET /api/margin/bonus-credit/report`, `GET /api/margin/bonus-credit/campaign-report`, and `POST /api/margin/bonus-credit/clawback`; these inherit the existing `/api/margin/**` funds security classification.
- `BonusCreditExpiryScheduler` provides a disabled-by-default scheduler entry via `bonus-credit.expiry-enabled`.
- `BonusCreditClawbackScheduler` provides a disabled-by-default campaign clawback policy via `bonus-credit.clawback-policy.*`.
- `TurnoverService` records `TradeExecuted` facts into `TurnoverRecord` with uid, account, symbol, strategy, order, match, sequence, quantity, price, and notional dimensions.
- `TurnoverService` can summarize turnover and return limited drill-down records by uid with optional symbol, strategy, market-maker, and match filters.
- `TurnoverReconciliationService` compares turnover records with trade tape for a uid + matchId and reports missing/mismatched trade facts.
- `MarginController` exposes `GET /api/margin/turnover/summary`, `GET /api/margin/turnover/records`, and `GET /api/margin/turnover/reconciliation` for operations and campaign reporting baselines.
- `OrderService` can optionally write turnover facts after idempotent trade accounting.
- `V11__turnover_records.sql` adds the durable turnover read model with indexes for uid, symbol, strategy, market-maker, and match lookups.
- `V12__bonus_credit_grants.sql` adds the durable bonus grant read model with uid/asset/status/expiry indexes.
- Tests cover bonus lifecycle separation from cash, clawback capping, turnover fact creation, and match-level turnover summary.

## Remaining Work

- Wire first-class product/order metadata into all future bonus consumption call sites as those flows start consuming bonus credits.
- Add first-class `strategyId` / `marketMakerId` fields to order entry instead of temporarily deriving strategy from `clientOrderId`.
- Extend turnover reconciliation from match-level trade-tape checks to scheduled ledger-ref reconciliation.
- Add exportable bonus campaign reports and paged/exportable turnover reports.

## Acceptance Criteria

- Bonus credit cannot be mixed silently with real cash.
- Expired or clawed-back bonus credit is auditable.
- Turnover totals can be compared against trade tape and ledger refs.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
