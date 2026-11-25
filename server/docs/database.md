# 資料庫與 Migration 慣例

本目錄說明 LumiX 如何整理資料庫 migration。

## 目前慣例

- Migration SQL 放在 `server/src/main/resources/db/migration/`。
- Migration 名稱必須可預測且有版本。
- 目前尚未上線，2026-11-25 已將原 V001–V015 收斂為單一 `V001__baseline.sql`；舊版號不是現行升級路徑。
- 此 baseline 一旦部署至共享或正式環境即不可再改寫，之後的 schema 變更必須新增版本化 migration。

## 範圍界線

- 本目錄只規範 migration 與資料庫文件位置；不改變任何 schema 或 runtime 行為。

## Baseline 與工具行為

- 目前 schema 以單一 `V001__baseline.sql` 建立完整最終狀態；Flyway 在空資料庫只應看見並套用這一筆 migration。
- migration SQL 必須清楚標示 precision、constraint 與 foreign key；認證 migration 不得保存明文密碼、session secret 或密碼重設 token，只允許不可逆雜湊。
- baseline 只描述目前 schema，不保留舊版本升級或歷史 compatibility 的中間步驟。
- Flyway 從 application classpath 的 `server/src/main/resources/db/migration/` 掃描 SQL migration；該目錄只存放可執行 SQL，說明文件統一留在本目錄。
- 空資料庫直接執行 baseline 後即可由 current application 使用。
- `db/init.sql` 是空資料庫的結構入口，只引用版本化 DDL migration；一般應用啟動仍以 Flyway 為準，不得把配置資料混入其中。
- `db/seed.sql` 只保存可重複執行的初始配置資料。只有明確設定 `LUMIX_DATABASE_SEED_ENABLED=true` 才會在 Flyway 完成後執行，正式環境預設關閉。
