# infra/redis

Redis repository implementations。

目前內容：
- Account、Order、Position、Snapshot、WalletLedger、WalletTransfer。
- `RedisOrderRepository` 維護 per-user order list/set 與全域 `ord:all` index；REST-mode matching book recovery 會用全域 open orders fallback 重建 in-memory depth。
- Idempotency、Outbox、DLQ。
- `RedisKeyNamespace` 統一 key prefix / version namespace。

目前狀態：
- Redis 存放 MVP hot state；長期金融紀錄 production 應移到 durable DB schema。
- key schema 文件在 `docs/*/redis-key-schema.md`。

注意：
- 不要使用 `KEYS` 做 production 查詢；`RedisOrderRepository` 只在 `ord:all` 舊資料缺失時用 user-list scan 作相容 fallback，production 應依賴全域 index。
- 新 repository 要同步補 Redis key schema 文件與 migration 策略。
