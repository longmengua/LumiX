# 非功能需求

## Reliability

- All effectful commands must be idempotent.
- Database trans動作 boundaries must be explicit.
- Outbox delivery must be retryable.
- Workers must tolerate duplicate messages.

## Security

- Admin 動作s require audit trail.
- 提款al signing requires least privilege and separation.
- 密鑰不得出現在日誌或資料庫表中。
- Risk bypass must require human approval.

## Performance

- Trading APIs should avoid unnecessary cross-service round trips.
- 餘額讀取應使用投影資料表。
- Matching core should be deterministic and benchmarkable.

## Auditability

- 帳本, order, trade, withdrawal, deposit, admin 動作, and risk decision must be queryable by correlation ID.
- Data correction must be append-only where funds are involved.
