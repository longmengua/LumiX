# src/test

測試根目錄。

目錄索引：
- `java/com/example/exchange/`：JUnit 5 測試主體。
- `java/com/example/exchange/application/service/`：P0 帳務、下單、風控、認證、做市商報價與 command boundary。
- `java/com/example/exchange/domain/util/`：P0 敏感資料遮罩工具。
- `java/com/example/exchange/infra/matching/`：in-memory 撮合引擎規則。
- `java/com/example/exchange/interfaces/web/`：P0 API auth、protected API security、WebSocket 與 public market API。

目前狀態：
- 使用 JUnit 5 / AssertJ。
- 只保留 P0 可執行測試；P1-P5 刪除清單與恢復依據見 `docs/ai/test-priority.md`。
- 測試類別會用 `@DisplayName` 寫出案例意圖，測試內只在 setup 容易誤解時加註解。

注意：
- 新增測試前先在 `docs/ai/test-priority.md` 標級；只有 P0 預設保留在主測試集。
