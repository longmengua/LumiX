# P29-R01 — API Runtime Infrastructure Topology Foundation

## 狀態

```text
COMPLETED_FOR_INFRASTRUCTURE_FOUNDATION
HUMAN_REVIEW_REQUIRED: yes
```

## 目標

為後續 user/auth 與 API runtime 提供實際的 Docker Compose、PostgreSQL primary、可選 read replica routing 與 Redis standalone/cluster client topology。此 task 不提供任何 public business endpoint、帳號認證、資金或交易能力。

## 相依與範圍

```text
依賴：P12 migration foundation、P29 API admission contract
包含：container build、health check、secret environment boundary、database/Redis topology configuration
不包含：production deployment、replica provisioning、Redis cluster provisioning、auth runtime、rate-limit runtime、任何資金或交易寫入
```

## 完成準則

1. `docker compose config` 可通過。
2. server 使用 PostgreSQL primary 執行 Flyway。
3. SINGLE 與 READ_WRITE_SPLIT datasource routing 設定可 fail-closed 驗證。
4. Redis connection factory 可依 standalone/cluster 明確配置。
5. Compose build、server health 與前端 build 有實測 evidence。
6. 文件說明 secret、rollback、replica/cluster 未完成的營運前提。

## 驗證結果

```text
PASS  docker compose config
PASS  Docker image build（web / server）
PASS  Compose PostgreSQL、Redis、server health 與 web startup
PASS  Flyway 8 migrations
PASS  Redis PING
PASS  /actuator/health 與 SPA /login
PASS  未提供 replica 時 READ_WRITE_SPLIT fail-closed
PASS  web typecheck
```

## 人工審核重點

- 所有 production secret 只能來自受管 secret store。
- read replica 的資料新鮮度與 read-after-write 一致性不可由 application configuration 單獨保證。
- Redis cluster 與 PostgreSQL replica 的實際 provisioning、網路 ACL、TLS、備援與演練需由基礎設施責任人驗證。
