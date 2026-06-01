# Task: Auditable Ledger Reconciliation

Status: `doing`

## Goal

Harden the current ledger and reconciliation baseline into an auditable accounting book with immutable journals, trial balance, replay comparison, exception workflow, and finance reports.

## Scope

- Immutable journal constraints and append-only rules.
- Trial balance by asset/account code.
- Replay comparison between ledger-derived and stored balances.
- Reconciliation exception states and operator workflow.
- Finance-facing daily reports for ledger, fees, funding, liquidation, bonus credit, and transfers.

## First Implementation Slice

1. [x] Inspect current wallet ledger journal and reconciliation report models.
2. [x] Add trial-balance read model/service.
3. [x] Add reconciliation exception state model.
4. [x] Add tests for balanced postings and mismatch classification.

## Progress

- `TrialBalanceService` can calculate total debit/credit and account-code lines from wallet ledger postings, then persist date/uid/asset scoped snapshots.
- `TrialBalanceReport` / `TrialBalanceLine` / `TrialBalanceSnapshot` provide finance-facing read models for user/asset scoped trial balance and daily close snapshots.
- Wallet ledger journal entries persist a hash chain with `previous_hash` and `entry_hash` for tamper-evidence verification.
- `ReconciliationReportIssue` now has `status`, `owner`, and `resolvedAt` fields, defaulting new issues to `OPEN`.
- `ReconciliationIssueWorkflowService` supports claim, resolve, reopen, and open-issue queue queries.
- `RecoveryController` exposes admin endpoints under `/api/recovery/reconcile/issues/...` for issue workflow.
- `WalletLedgerReplayService.compareAccountDetails` returns structured account/replay/delta mismatches per balance component.
- `RecoveryController` exposes `/api/recovery/reconcile/ledger/{uid}/compare` for structured replay comparison.
- `ReconciliationIssueWorkflowChanged` records claim/resolve/reopen workflow audit events.
- `FinanceReportService` can generate a UTC daily durable-ledger report grouped by reason, asset, and account code.
- `RecoveryController` exposes `GET /api/recovery/finance/daily-report?date=YYYY-MM-DD` and `GET/POST /api/recovery/finance/trial-balance/snapshot`.
- `FinanceReportService.categoryReport` and `GET /api/recovery/finance/category-report` provide fee/funding/liquidation/bonus/transfer category exports.
- `FinanceExportService`, `FinanceExportScheduler`, and `GET /api/recovery/finance/category-export-batch` provide a disabled-by-default daily category export job and operator-visible batch report.
- `LedgerArchiveEligibilityService` and `GET /api/recovery/finance/ledger-archive-eligibility` report whether ledger hot-path delete preconditions are met before any archive purge.
- `LedgerArchiveManifestService` and `GET /api/recovery/finance/ledger-archive-manifest` generate date-scoped archive manifests with counts, checksum, restore instructions, and delete eligibility.
- `LedgerArchiveManifestService` also exposes restore smoke, archived date-range replay validation, and immutable delete guard through `/api/recovery/finance/ledger-archive-restore-smoke`, `/api/recovery/finance/ledger-archive-replay-validation`, and `/api/recovery/finance/ledger-archive-delete-guard`.
- `RecoveryController` exposes `GET /api/recovery/reconcile/ledger/tamper-evidence` to verify durable journal hash-chain integrity.
- `V20__wallet_ledger_journal_constraints.sql` adds SQL constraints for wallet ledger schema version, nonblank entry dimensions, positive posting lines, and nonblank posting dimensions.
- [../../en/finance-operator-runbook.md](../../en/finance-operator-runbook.md) documents daily close, archive smoke, replay validation, and unbalanced report handling.
- `V13__reconciliation_issue_workflow.sql` adds issue workflow columns and indexes.
- Tests cover balanced trial balance aggregation, unbalanced posting detection, default reconciliation issue workflow state, daily category export batches, archive manifest restore smoke, archived date-range replay validation, and archive delete guard approval/blocking.

## Future Hardening

- Add broader operational controls around archive object storage and operator approvals.

## Acceptance Criteria

- Ledger postings can produce a trial balance.
- Reconciliation issues have stable status and ownership fields.
- Replay comparison reports explain mismatches without relying on logs.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
