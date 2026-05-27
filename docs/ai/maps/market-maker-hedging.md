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

## First Implementation Slice

1. Define market-maker account/profile model and risk limits.
2. Define hedge venue adapter interface with a fake/in-memory adapter for tests.
3. Add a quote/hedge command model and service boundary.
4. Add audit events for hedge decision, hedge order submitted, hedge fill, hedge failure.
5. Add tests covering exposure aggregation, kill switch, and slippage rejection.
