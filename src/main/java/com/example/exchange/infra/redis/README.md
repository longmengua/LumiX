# infra/redis

Redis repository implementations。

目前內容：
- Account、Order、Position、Snapshot、WalletLedger、WalletTransfer。
- Idempotency、Outbox、DLQ。
- `RedisKeyNamespace` 統一 key prefix / version namespace。

目前狀態：
- Redis 存放 MVP hot state；長期金融紀錄 production 應移到 durable DB schema。
- key schema 文件在 `docs/*/redis-key-schema.md`。

注意：
- 不要使用 `KEYS` 做 production 查詢；需要掃描時維護 index。
- 新 repository 要同步補 Redis key schema 文件與 migration 策略。
