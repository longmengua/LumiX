<!-- File purpose: operator runbook for finance export, ledger archive restore smoke, and unbalanced daily reports. Chinese version: ../zh-TW/finance-operator-runbook.md. -->
# Finance Operator Runbook

Use this runbook when closing a UTC finance day or investigating an unbalanced durable-ledger report.

## Daily Close Flow

1. Generate the durable ledger daily report:
   `GET /api/recovery/finance/daily-report?date=YYYY-MM-DD`
2. Generate all category export reports:
   `GET /api/recovery/finance/category-export-batch?date=YYYY-MM-DD`
3. Generate archive eligibility and manifest:
   `GET /api/recovery/finance/ledger-archive-eligibility?date=YYYY-MM-DD`
   `GET /api/recovery/finance/ledger-archive-manifest?date=YYYY-MM-DD`
4. Run restore smoke and replay validation before any hot-path delete:
   `GET /api/recovery/finance/ledger-archive-restore-smoke?date=YYYY-MM-DD`
   `GET /api/recovery/finance/ledger-archive-replay-validation?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD`
5. Run the immutable delete guard and only proceed when `approved=true`:
   `GET /api/recovery/finance/ledger-archive-delete-guard?date=YYYY-MM-DD`

## If The Daily Report Is Unbalanced

1. Stop archive/delete work for the affected date.
2. Capture `entryCount`, `totalDebit`, `totalCredit`, and the unbalanced report lines.
3. Run ledger tamper evidence:
   `GET /api/recovery/reconcile/ledger/tamper-evidence`
4. Run account replay comparison for affected users/assets where known:
   `GET /api/recovery/reconcile/ledger/{uid}/compare?asset=USDT`
5. Open or claim reconciliation issues before manual compensation:
   `GET /api/recovery/reconcile/issues/open`
6. After correction, rerun daily report, category export batch, archive restore smoke, replay validation, and delete guard.

## Scheduler

`FinanceExportScheduler` is disabled by default with `finance.export.enabled=false`. Enable only after daily report balance checks, archive restore smoke, alert routing, and operator ownership are confirmed.
