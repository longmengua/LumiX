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
- Configurable uid+symbol order-entry fixed-window frequency limit, disabled by default.
- Tick size, lot size, min notional, price band, max order size, max open orders.
- Balance, leverage, exposure, position notional, client order id deduplication.
- Reduce-only reducible quantity.

Remaining production TODO:
- External API idempotency coverage where risk depends on external systems.
- Multi-instance deployments should replace the local pre-trade frequency counter with Redis or gateway shared counting.

## Funds And Ledger

- API: `interfaces.web.controller.MarginController`
- Services: `MarginService`, `WalletLedgerService`, `WalletLedgerReplayService`, `FinanceReportService`
- Bonus credit: `WalletLedgerService` bonus-credit methods with `USER_BONUS_AVAILABLE`, `BonusCreditService`, `BonusCreditReport`, `BonusCreditCampaignReport`, `BonusCreditCampaignExport`, `BonusCreditProperties`
- Bonus expiry scheduler: `application.scheduler.BonusCreditExpiryScheduler`
- Turnover: `TurnoverService`, `TurnoverReconciliationService`, `TurnoverStore`, `TurnoverSummary`, `TurnoverExportReport`, `TurnoverReconciliationReport`
- Hot state: `infra.redis.RedisAccountRepository`, `RedisWalletLedgerRepository`, `RedisWalletTransferRepository`
- Durable journal: `domain.repository.jpa.JpaWalletLedgerJournal`
- Turnover store: `domain.repository.jpa.JpaTurnoverStore`
- Bonus grant store: `domain.repository.jpa.JpaBonusCreditGrantStore`
- Models: `Account`, `WalletLedgerEntry`, `WalletLedgerPosting`, `WalletTransfer`
- Turnover model: `TurnoverRecord`, `TurnoverSummary`, `TurnoverRecordEntity`
- Bonus grant model: `BonusCreditGrant`, `BonusCreditGrantRecord`
- Migrations: `V3__wallet_ledger_journal.sql`, `V11__turnover_records.sql`, `V12__bonus_credit_grants.sql`
- Tests: `MarginServiceTest`, `WalletLedgerReplayServiceTest`, `WalletLedgerServiceTest`, `BonusCreditServiceTest`, `TurnoverServiceTest`

Ledger concerns:
- Order reserve, position margin, fee, rebate, realized PnL, funding, liquidation shortfall, deposit, withdrawal are explicit accounting entries.
- Bonus credit grant, consume, expiry, and clawback are explicit ledger entries under `USER_BONUS_AVAILABLE`.
- Bonus credit is not added to `Account.crossBalance`, so promotional funds cannot silently mix with real cash.
- Bonus grant batches track remaining amount and expiry; consumption uses expiry FIFO and expiry scanning is disabled by default.
- `bonus-credit.eligibility` can gate consume by allowed/blocked symbol, allowed order type, and allowed expense account; it is disabled by default.
- `bonus-credit.clawback-policy` can auto-clawback a configured campaign/asset with a per-run cap; it is disabled by default.
- `MarginController` exposes bonus-credit user report, campaign report/export, clawback, and turnover summary/drill-down/export/reconciliation APIs under `/api/margin/**`, which keeps them in the funds security classification.
- Turnover facts are derived from processed `TradeExecuted` events and keep uid, account, symbol, first-class order strategy/market-maker tags, order, match, sequence, quantity, price, and notional dimensions.
- Turnover summaries, limited record drill-downs, and export rows can be queried by uid with optional symbol, strategy, market-maker, and match filters.
- Turnover reconciliation can compare uid + matchId turnover records with trade tape order/price/qty/notional facts, report order strategy/market-maker tags, and, when a durable wallet ledger journal is available, flags missing same-match ledger refs.
- `TurnoverReconciliationScheduler` can run disabled-by-default recent-window batch reconciliation with `turnover.reconciliation.*` settings.
- Replay compare endpoint verifies ledger-derived balances against stored account balances.
- Finance daily report summarizes durable ledger journal postings by reason, asset, and account code for a UTC report date.
- Finance category report filters the daily durable-ledger report by fee, funding, liquidation, bonus, or transfer reason sets.
- `LedgerArchiveEligibilityService` and `/api/recovery/finance/ledger-archive-eligibility` enforce ledger hot-path delete preconditions: retention window closed, hash-chain clean, balanced daily report, and non-empty candidate set.
- `LedgerArchiveManifestService` and `/api/recovery/finance/ledger-archive-manifest` generate date-scoped ledger archive manifests with source row counts, posting counts, aggregate checksum, restore instructions, and delete eligibility.
- `MarginService.recordDepositCallback` uses `WalletTransfer.externalRef` to replay duplicate chain/bank callbacks without double ledger posting.
- Manual-review transfers can be owner-claimed, and `transferReconciliation` projects transfer-vs-ledger ref matches for operations review.
- `PlaceOrderUseCase`, `CancelOrderUseCase`, `AmendOrderUseCase`, and `CancelReplaceOrderUseCase` now enter `CommandTransactionBoundary` in Spring runtime, so order reserve, matching side effects, ledger writes, order updates, and outbox rows share command-level database transaction boundaries.
- `CancelReplaceOrderUseCase` owns an outer boundary for cancel original plus replacement place, avoiding a database half-commit where original cancel succeeds but replacement order fails.
- `LiquidateUseCase` now enters `CommandTransactionBoundary` in Spring runtime before liquidation mutates position, ledger, insurance/ADL coverage, and audit events.
- `ExecuteAdlUseCase` now enters `CommandTransactionBoundary` in Spring runtime before ADL execution mutates position, ledger, execution records, and audit events.

Remaining production TODO:
- Stronger database constraints, audit retention, replay validation.
- Stronger alert delivery and worker locking for scheduled turnover reconciliation.
- Auditable accounting book with trial balance and reconciliation exception workflow.
- ADL DB-commit vs Redis hot-state repair rules are documented in `docs/en/redis-key-schema.md` and `docs/zh-TW/redis-key-schema.md`.

## Funding, Liquidation, Reconciliation

- Account risk: `AccountRiskService`, `AccountRiskSnapshotService`
- Funding: `FundingRateService`
- Liquidation: `LiquidationService`, `LiquidationScanService`
- Insurance fund: `InsuranceFundService`
- ADL ranking/planning/execution: `AdlRankingService`, `AdlDeleveragingPlanner`, `AdlForcedExecutionService`, `AdlQueueExecutionService`, `AdlInsuranceReconciliationService`, `AdlExecutionStore`, `JpaAdlExecutionStore`
- Reconciliation: `ReconciliationService`, `ReconciliationReportService`
- Trial balance: `TrialBalanceService`, `TrialBalanceReport`, `TrialBalanceLine`, `TrialBalanceSnapshot`, `TrialBalanceSnapshotStore`, `JpaTrialBalanceSnapshotStore`
- Liquidation audit event: `LiquidationDecisionRecorded`
- Migrations:
  - `V4__reconciliation_reports.sql`
  - `V6__account_risk_snapshots.sql`
  - `V13__reconciliation_issue_workflow.sql`
- Tests:
  - `AccountRiskServiceTest`
  - `AccountRiskSnapshotServiceTest`
  - `RiskSettlementServiceTest`
  - `InsuranceFundServiceTest`
  - `AdlRankingServiceTest`
  - `AdlDeleveragingPlannerTest`
  - `AdlForcedExecutionServiceTest`
  - `AdlQueueExecutionServiceTest`
  - `ReconciliationReportServiceTest`
  - `TrialBalanceServiceTest`

Current liquidation/ADL behavior:
- `LiquidationService` publishes `LiquidationDecisionRecorded` for liquidation decisions and `PositionLiquidated` when a position is closed.
- `LiquidationScanService` scans open positions and delegates oracle-based liquidation decisions.
- Scanner routing supports configurable batch size and isolates per-position failures so one bad symbol/config does not stop the whole batch.
- `AdlRankingService` provides deterministic ranking by profit rate, effective leverage, notional, and uid.
- `AdlDeleveragingPlanner` converts ranked candidates and ADL shortfall into deterministic reduce steps.
- `AdlForcedExecutionService` consumes ADL plans, validates candidate quantities before mutation, force reduces selected positions, writes realized-PnL and `adl_forced_loss` ledger postings, publishes execution audit events, and uses durable execution records for command id idempotency when configured.
- `InsuranceFundService` enqueues ADL shortfalls idempotently by `liquidationId` through `AdlQueueStore`, preserving any existing operator claim on duplicate retry/replay; Spring runtime uses `JpaAdlQueueStore` and unit tests can use `InMemoryAdlQueueStore`.
- `InsuranceFundService.stuckAdlClaims(...)` and `GET /api/risk/adl-queue/stuck-claims` expose claimed ADL entries older than an operator threshold for alert/report wiring.
- `GET /api/risk/adl-executions` reports recent forced-deleveraging outcomes from durable execution records when available.
- `AdlInsuranceReconciliationService` and `GET /api/risk/adl-insurance-reconciliation` compare open ADL queue shortfalls against liquidated-position ADL/insurance coverage.
- `AdlQueueExecutionService` consumes queued liquidation shortfalls, enforces queue owner guard when claimed, filters opposite-side candidates, ranks/plans ADL reduction, executes through `ExecuteAdlUseCase`, completes fully covered queue entries, keeps remaining notional on partial execution, and returns `ADL_NO_ELIGIBLE_CANDIDATES` without consuming the queue when no candidate can be reduced.
- Operator retry steps for stuck claims, partial executions, and no-candidate outcomes are documented in `docs/en/adl-operator-runbook.md` and `docs/zh-TW/adl-operator-runbook.md`.
- `RiskControlsProperties` exposes `liquidationHalt` and `liquidationManualReview` operator controls.

Remaining production TODO:
- Production scheduling/routing for liquidation scanners.
- Add production insurance-fund capital movement records, alert-backend delivery for stuck claims, and stronger operator assignment audit history.
- Alerts for reconciliation failure and unbalanced assets.

## Trial Balance And Reconciliation Issues

- `TrialBalanceService` aggregates wallet ledger postings by asset/account code and returns total debit, total credit, balanced flag, and net debit/credit lines.
- Trial-balance daily snapshots can be persisted and fetched through `/api/recovery/finance/trial-balance/snapshot` for finance close replay.
- Durable ledger entries now carry `previous_hash` and `entry_hash`; `/api/recovery/reconcile/ledger/tamper-evidence` verifies the journal hash chain and flags missing, previous-hash, or entry-hash mismatches.
- `ReconciliationReportIssue` now has `status`, `owner`, and `resolvedAt` fields for an operator workflow baseline.
- `ReconciliationIssueWorkflowService` and `/api/recovery/reconcile/issues/...` expose claim, resolve, reopen, and open-issue queue operations.
- `WalletLedgerReplayService.compareAccountDetails` and `/api/recovery/reconcile/ledger/{uid}/compare` return structured account/replay/delta mismatches.
- `ReconciliationIssueWorkflowChanged` is published for claim, resolve, and reopen audit trails.
- New issues created by `ReconciliationReportService` default to `OPEN`.

Remaining production TODO:
- Ledger archive exporter jobs and manifests.
