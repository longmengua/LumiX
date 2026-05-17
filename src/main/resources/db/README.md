# src/main/resources/db

資料庫 migration 根目錄。

目前狀態：
- `migration/` 放 Flyway SQL。
- 目前仍有 JPA/Hibernate MVP 痕跡；production TODO 要讓 Flyway 成為唯一 schema manager。

注意：
- 新資料表、index、constraint 必須走 migration。
