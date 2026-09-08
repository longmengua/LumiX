# Compose Runtime Foundation

## 目的與狀態

```text
P29-R01
COMPLETED_FOR_INFRASTRUCTURE_FOUNDATION
HUMAN_REVIEW_REQUIRED: yes
```

本文件與 root `docker-compose.yml` 建立可重複的 runtime foundation：React 前端、Spring Boot 後端、PostgreSQL primary 與 Redis。它讓後續 API / user runtime 可以接到實際服務，而不是前端 mock；它不是 production launch、帳號認證完成或資金服務啟用的證明。

## 本機／預備環境啟動

```text
cp .env.example .env
# 在 .env 置換 POSTGRES_PASSWORD；不得提交 .env
docker compose config
docker compose up --build -d
docker compose ps
curl http://127.0.0.1:8080/actuator/health
```

前端入口預設為 `http://127.0.0.1:8088`。Compose 將前端的 `/api/*` 同源反向代理至 server，避免把 backend host 寫進 browser bundle 或以 CORS 放寬安全邊界。

停止服務但保留測試資料：

```text
docker compose down
```

刪除 PostgreSQL 與 Redis volumes 是資料破壞操作，只有確認不再需要測試資料時才可執行：

```text
docker compose down --volumes
```

## PostgreSQL 讀寫 topology

```text
write / migration / 未標記 transaction
              |
              v
       PostgreSQL primary

@Transactional(readOnly = true)
              |
              +--> SINGLE ------------> PostgreSQL primary
              |
              +--> READ_WRITE_SPLIT --> 已驗證同步的 PostgreSQL replica
```

- `LUMIX_DATABASE_MODE=SINGLE` 是 Compose 預設；所有資料庫連線都落在 primary。
- `LUMIX_DATABASE_MODE=READ_WRITE_SPLIT` 時，必須提供 `LUMIX_DATABASE_REPLICA_JDBC_URL`、`USERNAME`、`PASSWORD`，且該 endpoint 必須是已監控 replication lag 的真正唯讀 replica。
- Flyway 永遠使用 `LUMIX_DATABASE_PRIMARY_*`，不會經過 read routing。
- 沒有 transaction 或非 `readOnly` transaction 一律走 primary，避免 command 被誤送至 replica。
- Compose 不建立假 replica；兩個獨立 PostgreSQL container 沒有 replication 不能構成讀寫分離。

## Redis topology

```text
application RedisConnectionFactory
              |
              +--> STANDALONE --> LUMIX_REDIS_HOST:LUMIX_REDIS_PORT
              |
              +--> CLUSTER ----> LUMIX_REDIS_CLUSTER_NODES
```

- `STANDALONE` 為 Compose 預設，使用單一 Redis container。
- `CLUSTER` 模式要求非空的逗號分隔 `host:port` 節點清單，例如 `redis-1:6379,redis-2:6379,redis-3:6379`。
- Redis password 只能由部署平台 secret 注入 `LUMIX_REDIS_PASSWORD`；`.env.example` 不包含可用密碼。
- 業務程式必須透過共用 `RedisConnectionFactory` 或 `RedisTemplate` 使用 Redis，不能自行散落建立 standalone client。

## 使用者認證與 API security runtime（P29-R02/R03）

- Compose 可提供 PostgreSQL 持久化 credential/session；密碼只能以 BCrypt 雜湊保存，Cookie secret 與 reset token 只保存 SHA-256 摘要。
- `LUMIX_AUTH_COOKIE_SECURE=false` 只允許本機 HTTP 驗證。交由 TLS ingress 對外時必須設為 `true`，並保持 web 與 `/api` 同源。
- Cookie 使用 `HttpOnly`、`SameSite=Strict`、`Path=/api`；前端 JavaScript 不可讀取 session 值。
- 忘記密碼預設 `LUMIX_AUTH_PASSWORD_RESET_SMTP_ENABLED=false`，endpoint 會 fail-closed。啟用時還必須由 secret manager 提供 `SPRING_MAIL_HOST`、`PORT`、`USERNAME`、`PASSWORD` 與 `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`；設定寄件地址，並將 `LUMIX_AUTH_PUBLIC_BASE_URL` 設為 HTTPS URL。
- 未啟用 SMTP 時 mail health indicator 會停用，避免空白預備參數讓整體 readiness 誤報；一旦啟用 SMTP，它會自動加入 health gate。
- SMTP 未配置時不可為了測試而把 reset token 寫進 Docker log、Redis、資料庫明文或 API response。
- `/api/v1/**` 預設 deny；只允許 register、login、forgot/reset password 匿名存取，其餘 route 必須先通過 session 鑒權。
- `LUMIX_SECURITY_REQUIRE_HTTPS=true` 時，server 拒絕未加密 API request，並要求 `LUMIX_AUTH_COOKIE_SECURE=true`；TLS ingress 必須為可信內部網路，因為它提供 `X-Forwarded-Proto`。
- API response 會加上 `Cache-Control: no-store`、`X-Content-Type-Options`、`X-Frame-Options` 與 `Referrer-Policy`。這些是防護補強，不能取代 TLS。

## Production handoff 的必要事項

- 以 secret manager 注入 PostgreSQL/Redis 密碼，不可使用 `.env`、image layer 或 git。
- 由 TLS reverse proxy / load balancer 對外發布 web；不可公開 PostgreSQL、Redis 或 server 管理埠。
- TLS 僅允許 1.3（必要相容時 1.2）、正式憑證與 HSTS；API 不得接受公網 HTTP 後再以 application 自製 payload 加密。
- 為 replica 增加 replication lag、failover、read-after-write 一致性與回切演練證據。
- 為 Redis cluster 增加 TLS、ACL、slot migration、節點故障與 reconnect 測試證據。
- 建置、SBOM/dependency scan、image signing、監控、備份還原與人工 launch sign-off 均尚未完成。

## Rollback

- application image 可以回滾到相容版本；不可假設資料庫 migration 可直接倒退。
- 新 migration 必須維持 backward compatible，並依 `deployment-runbook.md` 驗證 migration 狀態。
- topology 切回 `SINGLE` 前，必須先停止任何依賴 replica 的 read-only workload 並確認 primary 容量。

## 已完成驗證

```text
2026-09-09
PASS  docker compose --env-file .env.example config --quiet
PASS  Docker build: lumix-web / lumix-server
PASS  Compose startup: postgres、redis、server 均 healthy；web 已啟動
PASS  PostgreSQL: Flyway 8 migrations 成功套用
PASS  Redis standalone: PING -> PONG
PASS  server: /actuator/health -> UP
PASS  web: / 與 /login -> HTTP 200
PASS  READ_WRITE_SPLIT 未提供 replica JDBC URL 時，server fail-closed 拒絕啟動
PASS  web: npm run typecheck
```

未驗證項目：真實 PostgreSQL replica、replication lag、Redis cluster 節點／slot／故障轉移、TLS、外部 secret manager、production network ACL 與 production deployment。這些必須在相應基礎設施存在後另行驗證。
