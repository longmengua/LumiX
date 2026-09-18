# 資料庫 Migration

此目錄存放 LumiX 的 fresh-database schema baseline。

## 慣例

- 目前專案尚未上線，schema 以單一 `V001__baseline.sql` 建立完整最終狀態。
- 未來 schema 變更仍應使用版本化 Flyway migration；一旦 migration 已共享或已上線，才採 append-only 演進。
- migration SQL 必須清楚標示 precision、constraint 與 foreign key。
- 不要在 migration 中放入 runtime 資金移動邏輯。
- 認證 migration 不得保存明文密碼、session secret 或密碼重設 token；只允許不可逆雜湊。
- baseline 只描述目前 schema，不保留舊版本升級或歷史 compatibility 的中間步驟。

## 預期工具

- Flyway 會從 application classpath 掃描這個目錄。
- 空資料庫直接執行 baseline 後即可由 current application 使用。
