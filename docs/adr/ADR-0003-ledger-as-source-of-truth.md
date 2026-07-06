# ADR-0003 帳本 as Source of Truth

## 決定

帳本 journal and entries are the source of truth for funds. 餘額投影s are derived read models.

## Reason

A production exchange must be auditable, replayable, and repairable without mutating history.

## 後果

- Every money movement requires ledger entries.
- Balance caches must be rebuildable.
- Admin balance changes require adjustment journal and audit log.
