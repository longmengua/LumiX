# 結算合約

結算ment turns match results into ledger entries and order state changes.

## Sequence

```text
Match output
  -> validate reservations
  -> calculate gross amounts
  -> calculate fees
  -> append ledger journal
  -> capture reservations
  -> update order filled quantities
  -> update balance projection
  -> append outbox events
```

## Atomicity expectation

```text
ledger journal
reservation capture
order fill update
balance projection update
outbox append
```

These should commit together or be replayable without double settlement.

## Human review

Any change to settlement calculation, fee rounding, or 預留扣用 requires human review.
