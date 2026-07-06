# Trans動作 Boundary

## Rule

每個會造成業務副作用的 command 必須有明確 trans動作 boundary。

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

## Funds trans動作 rule

```text
Ledger entry append
Balance projection update
Reservation state update
Order state update
Outbox append
```

以上若同屬一個業務事件，必須在同一個 trans動作 或有可證明的補償與重放機制。

## Do not

- 不要在帳本分錄前更新餘額。
- 不要在資料庫提交前發布外部事件。
- Do not call chain broadcast inside an uncommitted database trans動作.
- Do not use distributed trans動作 unless explicitly designed and reviewed.
