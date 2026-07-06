# 可觀測性

## Required correlation IDs

```text
request_id
user_id
order_id
trade_id
journal_id
reservation_id
withdrawal_id
deposit_id
outbox_event_id
```

## Metrics

```text
api latency
api error rate
order accepted/rejected rate
reservation held/released/captured count
settlement success/failure count
ledger invariant check result
withdrawal pending/failed count
outbox lag
chain listener lag
```
