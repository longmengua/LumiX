# Non-Functional Requirements

## Reliability

- All effectful commands must be idempotent.
- Database transaction boundaries must be explicit.
- Outbox delivery must be retryable.
- Workers must tolerate duplicate messages.

## Security

- Admin actions require audit trail.
- Withdrawal signing requires least privilege and separation.
- Secrets must not appear in logs or database tables.
- Risk bypass must require human approval.

## Performance

- Trading APIs should avoid unnecessary cross-service round trips.
- Balance reads should use projection tables.
- Matching core should be deterministic and benchmarkable.

## Auditability

- Ledger, order, trade, withdrawal, deposit, admin action, and risk decision must be queryable by correlation ID.
- Data correction must be append-only where funds are involved.
