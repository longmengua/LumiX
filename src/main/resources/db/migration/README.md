# src/main/resources/db/migration

Flyway migration scripts。

目前內容：
- `V202606160642__exchange_schema_baseline.sql`：合併原 V1-V31 的完整開發 baseline schema，包含 core exchange、matching recovery、wallet ledger、risk/ADL、market data、Polymarket、customer auth、fee config、message center 等表與索引。

注意：
- 目前尚未正式發布 production schema；Docker volume 清空後可用單一 baseline 重新開始。
- 後續新增 migration 使用 `VyyyyMMddHHmm__business_summary.sql`，例如 `V202606161030__message_delivery_log.sql`。檔名中的 `V` 與雙底線 `__` 是 Flyway 規則，版本主體仍是 `yyyyMMddHHmm`。
- 已被發布或多人共用的 migration 不要重寫；若正式環境已套用，應新增下一個時間版本。
- Flyway 是 schema 唯一管理入口；Hibernate 只做 `validate`，不得用 `ddl-auto=update` 漂移 schema。
- production index、ledger schema、event projection schema 都應在這裡落地。
