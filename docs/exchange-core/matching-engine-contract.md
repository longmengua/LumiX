# 撮合引擎合約

## Input

```text
Accepted order
  order_id
  market_id
  side
  price
  quantity
  time_priority
  reservation_id
```

## Output

```text
Match result
  match_id
  taker_order_id
  maker_order_id
  price
  quantity
  maker_fee_rate
  taker_fee_rate
  sequence
```

## Determinism

Given the same 委託簿 state and same input order sequence, matching output must be identical.

## Boundary

Matching decides who trades with whom and at what price/quantity. Matching does not directly mutate ledger. 結算ment consumes match output and posts ledger entries.
