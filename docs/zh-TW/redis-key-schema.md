<!-- 檔案用途：Redis key schema、歸屬、TTL 與 migration 策略；英文版位於 ../en/redis-key-schema.md。 -->
# Redis Key Schema

這份文件記錄目前低延遲交易狀態使用的 Redis key。正式環境應把這些 key 視為需要版本化管理的資料契約。

English version: [../en/redis-key-schema.md](../en/redis-key-schema.md)

## 目前 Key

| Key pattern | 型別 | 歸屬 | Value | TTL |
| --- | --- | --- | --- | --- |
| `acc:{uid}` | String | `RedisAccountRepository` | `Account` object | 無 |
| `acc:index` | Set | `RedisAccountRepository` | 供對帳掃描使用的已知 account uid | 無 |
| `pos:{uid}` | Hash | `RedisPositionRepository` | field `symbolCode` -> `Position` object | 無 |
| `pos:open:index` | Set | `RedisPositionRepository` | 非零倉位 `{uid}:{symbolCode}` members | 無 |
| `order:{uuid}` | String | `RedisOrderRepository` | `Order` object | 無 |
| `ord:list:{uid}` | List | `RedisOrderRepository` | 依序保存 order id | 無 |
| `ord:set:{uid}` | Set | `RedisOrderRepository` | order id 去重集合 | 無 |
| `snap:{uid}` | String | `RedisSnapshotRepository` | 最新 `Snapshot` object | 無 |
| `wallet:ledger:{uuid}` | String | `RedisWalletLedgerRepository` | `WalletLedgerEntry` object | 無 |
| `wallet:ledger:uid:{uid}` | List | `RedisWalletLedgerRepository` | 使用者 ledger entry ids | 無 |
| `wallet:ledger:ref:{refId}` | List | `RedisWalletLedgerRepository` | 指定 reference 的 ledger entry ids | 無 |
| `outbox:index` | List | `RedisOutboxRepository` | Outbox event ids | 無 |
| `outbox:event:{uuid}` | String | `RedisOutboxRepository` | `OutboxEvent` object | 無 |
| `dlq:index` | List | `RedisDlqRepository` | DLQ event ids，最新在前 | 無 |
| `dlq:event:{uuid}` | String | `RedisDlqRepository` | `DlqEvent` object | 無 |
| `idempotency:{key}` | String | `RedisIdempotencyRepository` | marker value `1` | 有傳入 expiry 時使用有限 TTL |

## 正式環境規則

- 正式流量前設定 `REDIS_KEY_PREFIX`，例如 `mh:v1`，實際 key 會變成 `mh:v1:acc:{uid}`。預設空字串以保持向後相容。
- account、position、order、ledger、outbox、DLQ、snapshot 屬於營運狀態資料；除非已經有 archive 與 replay 流程，否則不應自動過期。
- Idempotency key 是例外：應依指令去重窗口設定有限 TTL。
- open position 掃描應讀取 `pos:open:index`。正式資料量除了一次性 migration tooling 外，不應使用 `KEYS pos:*`。
- List / Set index 必須和 object key 保持一致。若刪除或歸檔 `order:{uuid}`，也必須清理 `ord:list:{uid}` 與 `ord:set:{uid}`。
- schema 變更應使用新 namespace version，例如 `mh:v2:*`，或採 dual-read / dual-write migration。不要在相同 key pattern 下偷偷改 serialized object shape。

## Transaction Boundary 與 Recovery 規則

- 已有 durable schema 的 command 結果以 MySQL 為 authoritative store。Redis hot state 是 serving cache 或快速 index，除非某個 repository 尚未有 MySQL 替代實作。
- command transaction 必須先 commit database state 與 outbox rows，外部 publish 才能發生。若 database transaction rollback，Redis update 與 Kafka publish 都要視為無效，必須從 authoritative database 或 replay log 修復。
- 若 MySQL commit 成功但 Redis write 失敗，不要盲目重跑整個 command。應只 retry 或 rebuild Redis hot-state projection，來源是 durable orders、positions、ledger journals、matching logs 或 outbox events。
- 若 Redis 成功但 MySQL rollback，Redis value 視為 stale。Recovery tooling 必須能用 durable storage 覆寫 account、position、order、open-position index 與 order indexes。
- Idempotency key 只保護 command retry window，不等於 replay。若 command outcome 不確定，營運應先比對 durable command/outbox/ledger state，再決定是否接受 duplicate retry。
- Outbox/DLQ Redis keys 是 durable JPA repository 未啟用時的 legacy hot-state 實作。Production 應優先使用 MySQL outbox repository，讓 outbox rows 參與同一個 transaction boundary。

## Migration Backlog

- 各環境啟用 `REDIS_KEY_PREFIX`，並為既有未加 prefix 的資料規劃一次性 migration。
- 持續維護 account 與 open-position indexes，並補可從 durable storage 重建 index 的 repair tooling。
- 為歷史訂單、wallet ledger、outbox、DLQ、snapshot 補 archive job。
- 將長期保存的金融紀錄移到正式 database ledger schema，Redis 保留為 serving cache 或 hot-state store。
