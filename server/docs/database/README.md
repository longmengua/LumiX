# 資料庫慣例說明

本目錄說明 LumiX 如何整理資料庫 migration。

## 目前慣例

- Migration SQL 放在 `server/src/main/resources/db/migration/`。
- Migration 名稱必須可預測且有版本。
- 已共享的 migration 不可在原檔上修改。
- 新的資料庫變更必須使用新的 migration 檔。
- 回滾說明應寫在第 12 階段的 rollout 文件中，不要放進 runtime code。

## 範圍界線

- 這個任務只建立 migration tool 與目錄慣例。
- 尚未定義正式資料表。
- 不會實作 runtime 資金移動。
