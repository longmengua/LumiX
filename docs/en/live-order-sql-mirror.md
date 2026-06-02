<!-- File purpose: live order SQL mirror/index decision. Chinese version: ../zh-TW/live-order-sql-mirror.md. -->
# Live Order SQL Mirror

This document closes the P1 live-order SQL mirror/index decision for the current MVP.

## Decision

Use `order_lifecycle_projection` as the durable SQL mirror for live order reads. Do not add a separate `live_orders` table in this slice.

Why:
- `order_lifecycle_projection` is already written from `OrderLifecycleEvent` records and can be rebuilt from `order_lifecycle_events`.
- It contains the fields needed by live-order operations and audits: `order_id`, `uid`, `symbol`, `client_order_id`, `status`, `latest_stage`, quantity fields, price fields, and event timestamps.
- Flyway `V12__production_query_indexes.sql` already adds production query indexes for user/status and symbol/status access:
  - `idx_order_lifecycle_projection_uid_status_updated`
  - `idx_order_lifecycle_projection_symbol_status_time`
- Redis order keys remain hot-state serving indexes, not the production source of truth.

## Query Contract

Production live-order queries should use `order_lifecycle_projection` with these filters:

| Query | Required filter | Stable order |
| --- | --- | --- |
| User open orders | `uid`, `status in active statuses` | `updated_at desc`, `order_id` |
| User open orders by symbol | `uid`, `symbol`, `status in active statuses` | `updated_at desc`, `order_id` |
| Symbol open orders | `symbol`, `status in active statuses` | `last_event_at desc`, `order_id` |
| Client-order lookup | `client_order_id` plus `uid` when available | exact match |

Active status set should be kept in application code and tests with the order lifecycle state machine. Terminal states must remain queryable for audit and user history, but they are excluded from live-open-order screens.

## Rebuild And Drift Handling

`order_lifecycle_events` is the rebuild source. If Redis `order:{uuid}`, `ord:list:{uid}`, or `ord:set:{uid}` drift:

1. Do not rerun the original order command.
2. Read the latest `order_lifecycle_projection` state.
3. Rebuild only the Redis order object/index projection for non-terminal live orders.
4. If projection rows are missing or stale, rebuild them from `order_lifecycle_events` before repairing Redis.

This matches the Redis hot-state rule in `redis-key-schema.md`: Redis is a serving cache / fast index when a durable SQL projection exists.

## Future Upgrade Trigger

Create a dedicated `live_orders` table only if all are true:

- `order_lifecycle_projection` cannot satisfy p95/p99 latency after indexes and pagination are tuned.
- Query volume requires different retention or partitioning than lifecycle projection/history.
- The table can be maintained transactionally from lifecycle events or rebuilt deterministically from the event log.

Until those conditions are met, a separate mirror would add write amplification and drift risk without improving correctness.
