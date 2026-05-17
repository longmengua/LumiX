# src/main

這裡放 production code 與 runtime resources。

目錄：
- `java/`：Java 21 / Spring Boot 原始碼。
- `resources/`：application config、Flyway migration、static test console。

目前狀態：
- 應用仍是單體 MVP。
- Redis、Kafka、MySQL 在本機 compose 下使用；production 仍需拆分 worker 與強化 durable storage。
