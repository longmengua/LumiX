# Task: Market Maker Hedging

Status: `todo`

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

1. Define market-maker profile and risk limit model.
2. Define hedge venue adapter contract and fake adapter for tests.
3. Add exposure aggregation service.
4. Add slippage-control rejection tests.
5. Emit hedge decision and hedge order audit events.

## Acceptance Criteria

- Market-maker actions are isolated from ordinary user flows where needed.
- Hedge decisions are auditable and tied to exposure/trade refs.
- Kill switch blocks quoting and hedging safely.

## Read First

- [../../ai/maps/market-maker-hedging.md](../../ai/maps/market-maker-hedging.md)
- [../../ai/maps/order-matching.md](../../ai/maps/order-matching.md)
- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
