# ADR-0003 Ledger as Source of Truth

## Decision

Ledger journal and entries are the source of truth for funds. Balance projections are derived read models.

## Reason

A production exchange must be auditable, replayable, and repairable without mutating history.

## Consequences

- Every money movement requires ledger entries.
- Balance caches must be rebuildable.
- Admin balance changes require adjustment journal and audit log.
