<!-- 檔案用途：歷史訂單、成交、ledger、Kafka event、audit log 的 production archive strategy。 -->
# Archive Strategy

這份策略定義哪些資料可以離開 hot table 或 Redis、哪些資料必須保留查詢能力，以及 archived data restore 時以哪個來源為準。


## 範圍

| Data family | Hot source | Archive source | Minimum archive payload | Delete from hot path only after |
| --- | --- | --- | --- | --- |
| Historical orders | `order_lifecycle_events`、`order_lifecycle_projection`、Redis `order:*` | 依 `event_date/symbol/uid` 分區的 object storage，必要時加 cold SQL | lifecycle events、latest projection、client order id、status、quantities、timestamps | terminal state 已驗證、lifecycle event count 已 export、replay sample 通過、Redis secondary indexes 已清理 |
| Trades | matching event logs、market-data trade tape、Kafka `trade.executed` / `event.store.trade` | 依 `trade_date/symbol` 分區的 object storage | trade id/seq、maker/taker order ids、price、quantity、fee refs、source offsets | matching event offset checkpoint 已 export，且 finance/market-data downstream consumers 已追上 |
| Ledger entries | `wallet_ledger_entries`、`wallet_ledger_postings` | 依 `created_date/asset` 分區的 cold SQL 或 immutable object storage | entry、所有 postings、account codes、ref id、reason、schema version | retention window 的 trial balance 與 replay comparison 皆乾淨 |
| Kafka events | Kafka topics 與 outbox/DLQ tables | 依 `topic/event_date/partition` 分區的 object storage | key、payload、headers、schema version、partition、offset、produced timestamp | archive consumer group lag 為 0 且 offset manifest 已 commit |
| Audit logs | application/security/audit logs 與 audit event tables | 依 `log_date/service` 分區的 log archive storage | timestamp、request id、subject、action、result、reason、payload hash 或 event id | searchable index retention window 已過，且 export checksum 已記錄 |

## Retention Classes

| Class | Examples | Hot retention | Archive retention | Restore expectation |
| --- | --- | --- | --- | --- |
| Financial ledger | wallet ledger postings、funding/ADL/bonus postings | finance reporting 與 reconciliation window 結束前保留 hot | 依 finance policy 長期 immutable retention | 必須支援 exact replay/trial-balance verification |
| Trading audit | orders、trades、liquidations、hedge decisions | customer support 與 operational replay window 內保留 hot | 長期 audit retention | 必須能依 uid、symbol、order id、ref id 重建 order/trade |
| Market data history | depth deltas、trade tape、klines | 由 market-data retention config 控制 | 依 analytics/replay 需求歸檔 | restore 預設為分析用途，除非提升為 replay input |
| Operational events | outbox、DLQ、Kafka events、security logs | retry/compensation 與 alert window 內保留 hot | 保留 payload 與 offset/checksum manifests | 必須支援 incident review，安全時才 selective replay |

## Archive Manifest

每個 archive batch 必須寫 manifest：

- `archiveBatchId`、data family、schema version、environment、producer version。
- Time range、partition keys、source table/topic、source row 或 offset counts。
- 每個 exported object checksum，以及 batch aggregate checksum。
- 已涵蓋的最高 source ids 或 Kafka offsets。
- Delete eligibility flag 與 reviewer/operator id。
- Restore instructions 與預期 target table/topic。

## Delete And Restore Rules

- 先 archive，再驗 checksum，最後才刪 hot data。
- Delete 必須 idempotent，且以 manifest range 限縮，不得只用寬鬆的 `created_at < cutoff`。
- Ledger 與 trading audit archive 必須保留 schema version 與足夠欄位，讓 read model rebuild 不依賴 application default。
- Kafka archive 必須保留 topic、partition、offset、key、headers、payload、schema version。Replay 到 production topic 需要明確 compensation runbook。
- Restore 必須先寫 staging table 或 replay namespace，row count 與 checksum 對齊 manifest 後才能 promote。
- Redis hot-state deletion 必須遵守 [redis-key-schema.md](redis-key-schema.md)，確保 object keys 與 secondary indexes 一致。

## Open Implementation Work

- Historical orders、trades、ledger 的 archive exporter skeleton 已由 `ArchiveExporterService` / `ArchiveExporterScheduler` 提供；預設關閉，且只產生 export plans 與 ledger manifest/delete-guard checks，不會刪 hot-path data。
- 後續仍需替各 data family 實作 object-storage writers 與 hot-path delete jobs。Ledger finance category export、archive manifest generation、restore smoke、archived range replay validation 已有 `/api/recovery/finance/**` baseline。
- 為 order lifecycle、ledger replay、Kafka event payload 補 restore smoke tests。
- 把 archive checksum 接進 operations dashboards 與 incident runbooks。
