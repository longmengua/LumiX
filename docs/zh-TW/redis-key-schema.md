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

## 最終 TTL 與 Archive Policy

| Key family | Production TTL | Archive / deletion rule | Authoritative rebuild source |
| --- | --- | --- | --- |
| `acc:{uid}`, `acc:index` | 不自動過期 | account 活躍期間保留。只有完成 account closure、finance export 與 ledger replay 驗證後才能移除。 | Wallet ledger journal、account snapshots、reconciliation reports |
| `pos:{uid}`, `pos:open:index` | 不自動過期 | open position 永久保留。零倉位 closed position 可在 order/trade archive 與 risk snapshot export 後從 `pos:{uid}` 移除。 | Matching event log、order lifecycle projection、ledger replay、risk snapshots |
| `order:{uuid}` | open order 不設 TTL；terminal order 依 historical-order retention window 歸檔 | 刪 object key 前必須先歸檔 terminal order；同一個 maintenance job 必須同步清 `ord:list:{uid}` 與 `ord:set:{uid}`。 | Order lifecycle events/projection 與 matching command/event logs |
| `ord:list:{uid}`, `ord:set:{uid}` | 不自動過期 | 只能在 terminal order archive 後 trim 或 rebuild；index 不得指向已刪除的 `order:{uuid}`。 | 依 `uid` 查 order lifecycle projection |
| `snap:{uid}` | 不自動過期 | 保留 latest snapshot；成功產生新 snapshot 後原子替換。歷史 snapshot 應放 SQL `snapshots` table 或 archive storage。 | SQL snapshots table 與可 replay aggregate logs |
| `wallet:ledger:*` | Redis ledger 完全改由 SQL read 前不設 TTL | SQL ledger read 成為 authoritative 後，Redis ledger key 只能作 cache，可設 24h 這類有限 cache TTL；finance/audit retention 必須留在 MySQL/archive。 | Wallet ledger journal tables |
| `outbox:*`, `dlq:*` | legacy Redis adapter 啟用時不設 production TTL | Production 優先使用 MySQL outbox/DLQ。若仍有 Redis legacy keys，必須先 archive payload，且只在 compensation review 後刪 terminal published/DLQ records。 | MySQL outbox/DLQ tables 與 Kafka/archive consumers |
| `idempotency:{key}` | 必須使用 command dedupe window 的有限 TTL | 讓 Redis 自然過期；不得當作 business evidence 歸檔。Audit history 應由 durable command/outcome records 保存。 | Durable command logs、idempotency stores、ledger/outbox state |

Maintenance job 只有在有 authoritative rebuild source 時，才能刪除 object key 並同步清 secondary indexes。若 index cleanup 失敗，修復方式是從 authoritative source 重建 index，不是在 Redis 重新建立已歸檔 object。

## Transaction Boundary 與 Recovery 規則

- 已有 durable schema 的 command 結果以 MySQL 為 authoritative store。Redis hot state 是 serving cache 或快速 index，除非某個 repository 尚未有 MySQL 替代實作。
- command transaction 必須先 commit database state 與 outbox rows，外部 publish 才能發生。若 database transaction rollback，Redis update 與 Kafka publish 都要視為無效，必須從 authoritative database 或 replay log 修復。
- 若 MySQL commit 成功但 Redis write 失敗，不要盲目重跑整個 command。應只 retry 或 rebuild Redis hot-state projection，來源是 durable orders、positions、ledger journals、matching logs 或 outbox events。
- 若 Redis 成功但 MySQL rollback，Redis value 視為 stale。Recovery tooling 必須能用 durable storage 覆寫 account、position、order、open-position index 與 order indexes。
- Idempotency key 只保護 command retry window，不等於 replay。若 command outcome 不確定，營運應先比對 durable command/outbox/ledger state，再決定是否接受 duplicate retry。
- Outbox/DLQ Redis keys 是 durable JPA repository 未啟用時的 legacy hot-state 實作。Production 應優先使用 MySQL outbox repository，讓 outbox rows 參與同一個 transaction boundary。

## ADL Hot-State Repair 規則

- ADL forced execution 在 commit 後必須以 `adl_execution_records`、wallet ledger journal rows、持久化 position/account state 作為 authoritative state。
- Redis 或 in-memory ADL queue 只屬於營運 hot state。若 ADL execution 已 commit，但 queue completion 失敗，營運不應盲目重跑同一個 command；應先用 `commandId` 查 durable execution record，然後只移除或重建該 `liquidationId` 的 queue projection。
- 若 ADL execution rollback 發生在 operator claim 之後，應保留或恢復 claim state；只有確認不存在 durable execution record 後，才用同一個 `commandId` retry。
- 若 ADL execution 後 account、position 或 open-position Redis index 漂移，應從 durable position/account records 與 ledger replay 重建；不要用補打一筆 ADL ledger entry 的方式修 cache。
- 若 queue item 只被部分覆蓋，應保留剩餘 shortfall 並用新的 command id 執行下一次；不要用已完成 command id 承接新的 amount。

## Migration Backlog

- 各環境啟用 `REDIS_KEY_PREFIX`，並為既有未加 prefix 的資料規劃一次性 migration。
- 持續維護 account 與 open-position indexes，並補可從 durable storage 重建 index 的 repair tooling。
- 依最終 TTL/archive policy 實作歷史訂單、wallet ledger、outbox、DLQ、snapshot maintenance jobs。
- 將長期保存的金融紀錄移到正式 database ledger schema，Redis 保留為 serving cache 或 hot-state store。
