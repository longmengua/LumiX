# P12 Rollback Notes

Schema rollback in production is risky. Prefer forward fixes after data exists.

## Phase 12 rollback policy

- 目前只有 `V001__baseline.sql`，用於尚未上線且可清空的本機開發資料庫。
- 在此期間 schema 變更可直接整併進 V001，但每次都必須刪除本機資料庫與 Flyway history 後重新初始化。
- 一旦 schema 已共享、已有需保存資料或進入任何上線環境，禁止再改寫 V001；任何缺口都必須以新的 corrective migration 修正。

## 在正式資料之前

- 只限本機／開發環境：停止 application 後，drop and recreate LumiX database，再由 Flyway 重跑 V001。
- 不得以 Flyway repair 掩蓋 baseline 變更；清空資料庫是唯一允許的初始化方式。

## 在共享環境上線後

- Do not edit applied migration.
- Add new corrective migration.
- Preserve migration history.
- Document data repair script separately.
