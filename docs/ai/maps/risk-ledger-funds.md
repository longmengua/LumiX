# Risk, Ledger, And Funds Map

## Pre-Trade Risk

- Service: `application.service.RiskService`
- Config: `infra.config.RiskControlsProperties`, `DefaultSymbolConfigRepository`
- Symbol model: `domain.model.entity.SymbolConfig`
- Risk tiers: `domain.model.entity.SymbolRiskTier`
- Mark/index prices: `application.service.MarkPriceOracleService`
- Tests: `OrderAccountingIntegrationTest`, `MarkPriceOracleServiceTest`

Checks include:
- Global order-entry halt and reduce-only mode.
- Symbol suspension.
- Tick size, lot size, min notional, price band, max order size, max open orders.
- Balance, leverage, exposure, position notional, client order id deduplication.
- Reduce-only reducible quantity.

Remaining production TODO:
- Frequency limits and broader abuse controls.
- External API idempotency coverage where risk depends on external systems.

## Funds And Ledger

- API: `interfaces.web.controller.MarginController`
- Services: `MarginService`, `WalletLedgerService`, `WalletLedgerReplayService`
- Hot state: `infra.redis.RedisAccountRepository`, `RedisWalletLedgerRepository`, `RedisWalletTransferRepository`
- Durable journal: `domain.repository.jpa.JpaWalletLedgerJournal`
- Models: `Account`, `WalletLedgerEntry`, `WalletLedgerPosting`, `WalletTransfer`
- Migrations: `V3__wallet_ledger_journal.sql`
- Tests: `MarginServiceTest`, `WalletLedgerReplayServiceTest`

Ledger concerns:
- Order reserve, position margin, fee, rebate, realized PnL, funding, liquidation shortfall, deposit, withdrawal are explicit accounting entries.
- Replay compare endpoint verifies ledger-derived balances against stored account balances.

Remaining production TODO:
- Stronger database constraints, audit retention, replay validation.
- Bonus-credit / experience-fund ledger accounts, consumption priority, expiry, clawback, and reporting.
- Turnover tracking by user, account, symbol, strategy, and market-maker dimensions.
- Auditable accounting book with trial balance and reconciliation exception workflow.
- Chain/bank callbacks, manual-review ownership, transfer reconciliation projections.

## Funding, Liquidation, Reconciliation

- Account risk: `AccountRiskService`, `AccountRiskSnapshotService`
- Funding: `FundingRateService`
- Liquidation: `LiquidationService`
- Insurance fund: `InsuranceFundService`
- Reconciliation: `ReconciliationService`, `ReconciliationReportService`
- Migrations:
  - `V4__reconciliation_reports.sql`
  - `V6__account_risk_snapshots.sql`
- Tests:
  - `AccountRiskServiceTest`
  - `AccountRiskSnapshotServiceTest`
  - `RiskSettlementServiceTest`
  - `ReconciliationReportServiceTest`

Remaining production TODO:
- Liquidation scanning, execution routing, operational controls.
- Production ADL queue ranking, forced deleveraging execution, insurance-fund interaction, and audit events.
- Alerts for reconciliation failure and unbalanced assets.
