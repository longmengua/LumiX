<!-- File purpose: Production archive strategy for historical orders, trades, ledger, Kafka events, and audit logs. Chinese version: ../zh-TW/archive-strategy.md. -->
# Archive Strategy

This strategy defines what may leave hot tables or Redis, what must stay queryable, and which source is authoritative when archived data is restored.

中文版本：[../zh-TW/archive-strategy.md](../zh-TW/archive-strategy.md)

## Scope

| Data family | Hot source | Archive source | Minimum archive payload | Delete from hot path only after |
| --- | --- | --- | --- | --- |
| Historical orders | `order_lifecycle_events`, `order_lifecycle_projection`, Redis `order:*` | Object storage partitioned by `event_date/symbol/uid` plus cold SQL if available | lifecycle events, latest projection, client order id, status, quantities, timestamps | terminal state verified, lifecycle event count exported, replay sample passed, Redis secondary indexes cleaned |
| Trades | matching event logs, market-data trade tape, Kafka `trade.executed` / `event.store.trade` | Object storage partitioned by `trade_date/symbol` | trade id/seq, maker/taker order ids, price, quantity, fee refs, source offsets | matching event offset checkpoint exported and downstream finance/market-data consumers caught up |
| Ledger entries | `wallet_ledger_entries`, `wallet_ledger_postings` | Cold SQL or immutable object storage partitioned by `created_date/asset` | entry, all postings, account codes, ref id, reason, schema version | trial balance and replay comparison are clean for the retention window |
| Kafka events | Kafka topics and outbox/DLQ tables | Object storage partitioned by `topic/event_date/partition` | key, payload, headers, schema version, partition, offset, produced timestamp | consumer lag is zero for archive group and offset manifest is committed |
| Audit logs | application/security/audit logs and audit event tables | Log archive storage partitioned by `log_date/service` | timestamp, request id, subject, action, result, reason, payload hash or event id | searchable index retention window elapsed and export checksum recorded |

## Retention Classes

| Class | Examples | Hot retention | Archive retention | Restore expectation |
| --- | --- | --- | --- | --- |
| Financial ledger | wallet ledger postings, funding/ADL/bonus postings | Keep hot until finance reporting and reconciliation windows close | Long-term immutable retention per finance policy | Must support exact replay/trial-balance verification |
| Trading audit | orders, trades, liquidations, hedge decisions | Keep hot through customer support and operational replay windows | Long-term audit retention | Must support order/trade reconstruction by uid, symbol, order id, and ref id |
| Market data history | depth deltas, trade tape, klines | Controlled by market-data retention config | Archive when needed for analytics/replay | Restore is analytical unless promoted to replay input |
| Operational events | outbox, DLQ, Kafka events, security logs | Keep hot until retry/compensation and alert windows close | Retain payload plus offset/checksum manifests | Must support incident review and selective replay where safe |

## Archive Manifest

Every archive batch must write a manifest with:

- `archiveBatchId`, data family, schema version, environment, and producer version.
- Time range, partition keys, source table/topic, and source row or offset counts.
- Checksum per exported object and aggregate checksum for the batch.
- Highest source ids or Kafka offsets included.
- Delete eligibility flag and reviewer/operator id.
- Restore instructions and expected target table/topic.

## Delete And Restore Rules

- Archive first, verify checksum second, delete hot data last.
- Deletes must be idempotent and scoped by manifest range, not by broad `created_at < cutoff` alone.
- Ledger and trading audit archives must keep schema version and enough fields to rebuild current read models without relying on application defaults.
- Kafka archives must preserve topic, partition, offset, key, headers, payload, and schema version. Replay into production topics requires an explicit compensation runbook.
- Restores must write into a staging table or replay namespace first, then promote after row counts and checksums match the manifest.
- Redis hot-state deletion must follow [redis-key-schema.md](redis-key-schema.md) so object keys and secondary indexes stay consistent.

## Open Implementation Work

- Build archive exporter jobs for each data family. Ledger finance category export, archive manifest generation, restore smoke, and archived range replay validation have baselines under `/api/recovery/finance/**`.
- Add restore smoke tests for order lifecycle, ledger replay, and Kafka event payloads.
- Add archive checksums to operations dashboards and incident runbooks.
