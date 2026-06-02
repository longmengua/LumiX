<!-- 檔案用途：live order SQL mirror/index 決策。英文版本位於 ../en/live-order-sql-mirror.md。 -->
# Live Order SQL Mirror

本文件關閉目前 MVP 的 P1 live-order SQL mirror/index 決策。

## 決策

使用 `order_lifecycle_projection` 作為 live order 查詢的 durable SQL mirror。本 slice 不新增獨立的 `live_orders` 表。

原因：
- `order_lifecycle_projection` 已由 `OrderLifecycleEvent` 寫入，且可從 `order_lifecycle_events` 重建。
- 它已包含 live-order 操作與 audit 需要的欄位：`order_id`、`uid`、`symbol`、`client_order_id`、`status`、`latest_stage`、數量欄位、價格欄位與事件時間。
- Flyway `V12__production_query_indexes.sql` 已加入 user/status 與 symbol/status 的 production query indexes：
  - `idx_order_lifecycle_projection_uid_status_updated`
  - `idx_order_lifecycle_projection_symbol_status_time`
- Redis order keys 仍是 hot-state serving indexes，不是 production source of truth。

## 查詢契約

Production live-order query 應使用 `order_lifecycle_projection` 搭配以下 filters：

| 查詢 | 必要 filter | 穩定排序 |
| --- | --- | --- |
| 使用者 open orders | `uid`、`status in active statuses` | `updated_at desc`, `order_id` |
| 使用者單一 symbol open orders | `uid`、`symbol`、`status in active statuses` | `updated_at desc`, `order_id` |
| 單一 symbol open orders | `symbol`、`status in active statuses` | `last_event_at desc`, `order_id` |
| Client-order lookup | `client_order_id`，可用時加 `uid` | exact match |

Active status set 應由 application code 與 order lifecycle state machine 測試固定。Terminal states 仍需可供 audit 與使用者歷史查詢，但不列入 live-open-order 畫面。

## Rebuild 與 Drift 處理

`order_lifecycle_events` 是重建來源。若 Redis `order:{uuid}`、`ord:list:{uid}` 或 `ord:set:{uid}` 發生 drift：

1. 不要重跑原始 order command。
2. 讀取最新 `order_lifecycle_projection` state。
3. 只針對非終態 live orders 重建 Redis order object/index projection。
4. 若 projection rows 缺失或過舊，先從 `order_lifecycle_events` 重建 projection，再修 Redis。

這符合 `redis-key-schema.md` 的 Redis hot-state 規則：當 durable SQL projection 已存在時，Redis 是 serving cache / fast index。

## 後續升級條件

只有在以下條件全成立時，才新增獨立 `live_orders` 表：

- `order_lifecycle_projection` 經過 index 與 pagination 調整後仍無法滿足 p95/p99 latency。
- Query volume 需要不同於 lifecycle projection/history 的 retention 或 partitioning。
- 新表可從 lifecycle event transactionally 維護，或可從 event log deterministic rebuild。

在這些條件成立前，獨立 mirror 只會增加 write amplification 與 drift 風險，並不會提升正確性。
