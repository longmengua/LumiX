# Reservation State Machine

Reservation protects available balance before order or withdrawal effects are finalized.

## State diagram

```text
+-----------+
| REQUESTED |
+-----+-----+
      |
      v
+-----+-----+     insufficient funds     +--------+
| HELD      | -------------------------> | FAILED |
+--+-----+--+                            +--------+
   |     |
   |     +--- release unused ----------> +----------+
   |                                    | RELEASED |
   |                                    +----------+
   |
   +--- capture used -----------------> +----------+
                                        | CAPTURED |
                                        +----------+
```

## State definitions

```text
REQUESTED  request received but not held
HELD       amount removed from available balance and held for purpose
CAPTURED   held amount consumed by settlement or withdrawal
RELEASED   held amount returned to available balance
FAILED     hold did not happen
```

## Invariants

- HELD amount cannot exceed available balance at hold time.
- CAPTURED + RELEASED cannot exceed HELD.
- Reservation purpose must be explicit: ORDER, WITHDRAWAL, ADMIN_HOLD.
- Reservation must reference order_id, withdrawal_id, or admin_action_id.
- Reservation transitions must be idempotent.
