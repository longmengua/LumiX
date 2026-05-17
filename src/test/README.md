# src/test

測試根目錄。

目錄索引：
- `java/com/example/exchange/`：JUnit 5 測試主體。
- `java/com/example/exchange/application/service/`：帳務、下單、風控、出入金、outbox、metrics。
- `java/com/example/exchange/domain/util/`：checksum 與敏感資料遮罩工具。
- `java/com/example/exchange/infra/matching/`：in-memory 撮合引擎規則。
- `java/com/example/exchange/interfaces/web/`：API auth、IP/rate-limit、trace header。

目前狀態：
- 使用 JUnit 5 / AssertJ。
- 測試主要集中在 `java/com/example/exchange/`。
- 測試類別會用 `@DisplayName` 寫出案例意圖，測試內只在 setup 容易誤解時加註解。

注意：
- 新增 production-readiness baseline 時要補 focused unit/integration test。
