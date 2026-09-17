# 資料庫 Migration

此目錄存放 LumiX 的正式 schema migrations。

## 慣例

- 使用可預測且有版本的名稱，例如 `V001__create_identity_and_assets.sql`。
- 一旦 migration 已經共享，就維持 append-only。
- 在共享環境中，不要重寫已套用的 migration。
- 後續修正請新增一個 corrective migration。
- migration SQL 必須清楚標示 precision、constraint 與 foreign key。
- 不要在 migration 中放入 runtime 資金移動邏輯。
- 認證 migration 不得保存明文密碼、session secret 或密碼重設 token；只允許不可逆雜湊。
- 歷史 schema 與產品範圍不一致時，必須新增前向 corrective migration；例如 `V023__remove_spot_margin_accounts.sql` 只會刪除無任何會計或錢包證據的舊 MARGIN 容器，存在依賴時必須 fail-closed，不能刪改稽核資料。

## 預期工具

- Flyway 會從 application classpath 掃描這個目錄。
- 未來的 schema 變更都應該落在這裡，每個 change set 一個 migration。
