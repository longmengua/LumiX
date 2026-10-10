# 資料庫與 Migration 慣例

本目錄說明 LumiX 如何整理資料庫 migration。

## 目前慣例

- Migration SQL 放在 `server/src/main/resources/db/migration/`。
- Migration 名稱必須可預測且有版本。
- 已共享的 migration 不可在原檔上修改。
- 新的資料庫變更必須使用新的 migration 檔。
- 回滾說明應寫在第 12 階段的 rollout 文件中，不要放進 runtime code。

## 範圍界線

- 本目錄只規範 migration 與資料庫文件位置；不改變任何 schema 或 runtime 行為。
- 資金相關 schema 必須仍遵守 immutable ledger、precision 與既有 phase gate。

## Baseline 與工具行為

- 目前專案尚未上線，schema 以單一 `V001__baseline.sql` 建立完整最終狀態；未來 schema 變更仍應使用版本化 Flyway migration。一旦 migration 已共享或已上線，才採 append-only 演進。
- migration SQL 必須清楚標示 precision、constraint 與 foreign key；認證 migration 不得保存明文密碼、session secret 或密碼重設 token，只允許不可逆雜湊。
- baseline 只描述目前 schema，不保留舊版本升級或歷史 compatibility 的中間步驟。
- Flyway 從 application classpath 的 `server/src/main/resources/db/migration/` 掃描 SQL migration；該目錄只存放可執行 SQL，說明文件統一留在本目錄。
- 空資料庫直接執行 baseline 後即可由 current application 使用。
