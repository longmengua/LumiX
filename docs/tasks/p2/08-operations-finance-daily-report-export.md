# Task: Operations Finance Daily Report Export

Status: `todo`

## Goal

Add a daily export workflow for operations and finance summaries with deterministic windows, manifests, and restore/replay traceability.

## Scope

- Daily summary rows for fees, funding, liquidation, bonus credit, transfer, reconciliation status, and unresolved exceptions.
- Export manifest with date range, generation time, row counts, checksum, and source version.
- Disabled-by-default scheduler or manual trigger.
- Clear handling for reruns and duplicate manifests.

## First Implementation Slice

1. Reuse existing finance category export and reconciliation report baselines.
2. Add a daily summary manifest model or DTO.
3. Add service tests for deterministic window boundaries and duplicate rerun behavior.
4. Add docs for operator trigger and export storage assumptions.

## Acceptance Criteria

- Daily exports are idempotent for the same reporting date and source version.
- Manifest includes checksum and row count.
- Tests cover rerun, empty day, and unresolved reconciliation issue cases.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)

