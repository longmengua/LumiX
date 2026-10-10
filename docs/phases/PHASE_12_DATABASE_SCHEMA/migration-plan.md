# P12 遷移計畫

## 目前初始化策略

```text
V001__baseline.sql
```

## 遷移規則

- 本機開發資料庫尚未上線且沒有需保存資料時，所有 current schema 都收斂在 `V001__baseline.sql`，資料庫以清空後重新初始化處理。
- 一旦資料庫已共享、已有需保存資料或進入任何上線環境，禁止再改寫 V001；之後的 schema 變更必須以新的 append-only migration 提供。
- 高風險資料表與欄位必須保留 PostgreSQL comment；回滾與重建注意事項請更新 `rollback-notes.md`。
