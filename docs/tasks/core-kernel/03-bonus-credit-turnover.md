# Task: Bonus Credit And Turnover

Status: `todo`

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

1. Define bonus credit ledger account codes and posting reasons.
2. Add turnover record/read model.
3. Update trade accounting to emit turnover facts.
4. Add tests for bonus consumption and turnover reconciliation.

## Acceptance Criteria

- Bonus credit cannot be mixed silently with real cash.
- Expired or clawed-back bonus credit is auditable.
- Turnover totals can be compared against trade tape and ledger refs.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
