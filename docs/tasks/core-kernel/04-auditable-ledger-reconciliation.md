# Task: Auditable Ledger Reconciliation

Status: `todo`

## Goal

Harden the current ledger and reconciliation baseline into an auditable accounting book with immutable journals, trial balance, replay comparison, exception workflow, and finance reports.

## Scope

- Immutable journal constraints and append-only rules.
- Trial balance by asset/account code.
- Replay comparison between ledger-derived and stored balances.
- Reconciliation exception states and operator workflow.
- Finance-facing daily reports for ledger, fees, funding, liquidation, bonus credit, and transfers.

## First Implementation Slice

1. Inspect current wallet ledger journal and reconciliation report models.
2. Add trial-balance read model/service.
3. Add reconciliation exception state model.
4. Add tests for balanced postings and mismatch classification.

## Acceptance Criteria

- Ledger postings can produce a trial balance.
- Reconciliation issues have stable status and ownership fields.
- Replay comparison reports explain mismatches without relying on logs.

## Read First

- [../../ai/maps/risk-ledger-funds.md](../../ai/maps/risk-ledger-funds.md)
- [../../ai/maps/reliability-market-data.md](../../ai/maps/reliability-market-data.md)
- [../../ai/maps/persistence-tests.md](../../ai/maps/persistence-tests.md)
