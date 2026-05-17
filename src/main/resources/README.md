# src/main/resources

Runtime resources。

目前內容：
- `application.yml`：共用預設設定。
- `application-dev.yml`：本機開發設定。
- `application-prod.yml`：production profile 設定樣板。
- `db/migration/`：Flyway migration。
- `static/`：本機測試控制台。

注意：
- secrets 不應提交到這裡。
- 新 config properties 要同步 dev/prod 文件與預設值。
