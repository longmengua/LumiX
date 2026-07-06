# 資料庫 Migration

此目錄存放 LumiX 的正式 schema migrations。

## 慣例

- 使用可預測且有版本的名稱，例如 `V001__create_identity_and_assets.sql`。
- 一旦 migration 已經共享，就維持 append-only。
- 在共享環境中，不要重寫已套用的 migration。
- 後續修正請新增一個 corrective migration。
- migration SQL 必須清楚標示 precision、constraint 與 foreign key。
- 不要在 migration 中放入 runtime 資金移動邏輯。

## 預期工具

- Flyway 會從 application classpath 掃描這個目錄。
- 未來的 schema 變更都應該落在這裡，每個 change set 一個 migration。
