# src

這裡是主要程式碼與測試。

目錄：
- `main/`：Spring Boot 應用程式、resources、Flyway migration、static test console。
- `test/`：JUnit 測試，覆蓋安全、撮合、帳務、風控、outbox、observability baseline。

目前狀態：
- 架構採 DDD-style 分層：interfaces、application、domain、infra。
- 內部交易所與 Polymarket integration 共存在同一 Spring Boot app。
