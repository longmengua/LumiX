# src/main/resources/db/migration

Flyway migration scripts。

目前內容：
- `V1__core_v1_baseline.sql`：core-v1 乾淨 baseline schema，合併原 V1-V20 的可靠性、order lifecycle、ledger、reconciliation、matching replay/lease、turnover、bonus credit、market-maker、hedging、prediction / Polymarket tables。
- `V2__adl_execution_records.sql`：post-core-v1 ADL forced execution summary / idempotency records。
- `V3__market_data_sequence_checkpoints.sql`：post-core-v1 market-data stream sequence/checksum checkpoints。
- `V4__market_data_depth_deltas.sql`：post-core-v1 depth delta reconnect backfill records。
- `V5__market_data_trade_tape.sql`：post-core-v1 trade tape records。
- `V6__market_data_tickers.sql`：post-core-v1 ticker latest-state records。
- `V7__market_data_klines.sql`：post-core-v1 kline records。

注意：
- 目前尚未正式發布 production schema；Docker volume 清空後可用單一 baseline 重新開始。
- core-v1 之後若已對外發布 migration，後續變更應建立下一個 `V{n}__*.sql`，不要再修改既有版本。
- Flyway 是 schema 唯一管理入口；Hibernate 只做 `validate`，不得用 `ddl-auto=update` 漂移 schema。
- production index、ledger schema、event projection schema 都應在這裡落地。
