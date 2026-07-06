# 訂單生命周期

## 訂單 states

```text
NEW -> VALIDATED -> ACCEPTED -> OPEN -> PARTIALLY_FILLED -> FILLED
                    |            |             |
                    v            v             v
                 REJECTED     CANCELLED     CANCELLED
```

## 訂單 data requirements

```text
order_id
user_id
market_id
side
order_type
price
quantity
remaining_quantity
status
reservation_id
client_order_id
created_at
updated_at
```

## Idempotency

`client_order_id` or idempotency key must prevent accidental duplicate orders.

## Money safety

訂單 acceptance must not imply settlement. Funds must be reserved before order enters matching.
