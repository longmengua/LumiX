# Transaction Boundary

## Rule

每個會造成業務副作用的 command 必須有明確 transaction boundary。

```text
API request
  -> validate
  -> idempotency claim
  -> begin database transaction
  -> domain mutation
  -> outbox append
  -> commit
  -> async side effect delivery
```

## Funds transaction rule

```text
Ledger entry append
Balance projection update
Reservation state update
Order state update
Outbox append
```

以上若同屬一個業務事件，必須在同一個 transaction 或有可證明的補償與重放機制。

## Do not

- Do not update balance before ledger entry.
- Do not publish external event before database commit.
- Do not call chain broadcast inside an uncommitted database transaction.
- Do not use distributed transaction unless explicitly designed and reviewed.
