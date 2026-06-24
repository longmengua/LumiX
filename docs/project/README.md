<!-- 檔案用途：繁體中文專案說明；文件入口請見 docs/README.md。 -->
# Java21 Match Hub

Java 21 + Spring Boot 3.5 的交易與預測市場整合後端。專案目前同時包含「內部交易所核心」與「Polymarket 整合」兩條業務線，使用 DDD 分層組織程式碼，並以 MySQL、Redis、Kafka 作為本機開發基礎設施。

目前定位是可執行的交易核心 MVP，不是生產級交易所。撮合仍是 in-memory 實作，Kafka、Redis、MySQL 也以單節點本機開發模式啟動；若要上正式環境，還需要補強持久化撮合、風控限額、帳務審計、密鑰管理、觀測與壓測。

文件入口：[docs/README.md](../README.md)

## 文件分類

本頁是繁體中文 **產品文件** 入口。

| 分類 | 說明 | 連結 |
| --- | --- | --- |
| 產品文件 | 業務總覽、功能、模塊、API 與下單鏈路。 | 目前頁面 |
| 目前狀態 | 快速說明完成度、MVP baseline、production blocker 與近期優先順序。 | [current-state.md](current-state.md) |
| Core V1 Release | 有邊界的 core-v1 baseline freeze scope、release gate、smoke runbook 與驗收指令。 | [core-v1-release-checklist.md](core-v1-release-checklist.md) / [core-v1-smoke-runbook.md](../runbooks/core-v1-smoke-runbook.md) |
| 技術文件 | 架構、實作筆記、API curl 腳本與撮合引擎說明。 | [technical.md](technical.md) |
| 待辦清單文件 | 依優先級與領域整理的 production readiness roadmap。 | [todo.md](todo.md) |
| AI 文件 | 給 Codex/代理使用的精簡地圖與任務入口流程。 | [ai.md](ai.md) |
| 任務文件 | roadmap 與插單工作的可點名 task md。 | [task-index.md](task-index.md) |

## 技術棧

- Java 21
- Spring Boot 3.5.6
- Spring Web / Validation / WebSocket
- Spring Data JPA + MySQL 8.4
- Redis 7.4
- Kafka 7.6 KRaft single node
- Flyway
- OkHttp / Web3j
- Lombok
- Maven Wrapper

## 本機啟動

先啟動依賴服務：

```bash
docker compose up -d
```

Compose 會啟動：

| Service | Port | 用途 |
| --- | ---: | --- |
| MySQL | 3306 | JPA entity、Polymarket market/session/order 資料 |
| Redis | 6379 | 低延遲狀態、快照、idempotency、outbox/dlq |
| Kafka | 9092 | domain event、event store、Polymarket user event |
| Kafka UI | 8081 | 本機查看 topic、message、consumer group |

啟動應用程式：

```bash
./mvnw spring-boot:run
```

預設 profile 是 `dev`，會連到 `localhost:3306`、`localhost:6379`、`localhost:9092`。應用啟動後 API 預設在 `http://localhost:8080`。

常用維運指令：

```bash
docker compose ps
docker compose logs -f kafka
docker compose down
docker compose down -v
```

`docker compose down -v` 會清除 MySQL、Redis、Kafka 的本機資料 volume。

## 業務功能

### 內部交易所核心

- 下單、改單、cancel-replace、批量撤單、查詢掛單與歷史訂單。
- 撮合引擎支援 LIMIT / MARKET、GTC / IOC / FOK、部分成交、價格時間優先、self match prevention。
- 訂單簿深度、成交帶、ticker、kline、depth delta。
- 保證金入金/出金狀態機、全倉/逐倉劃轉、帳戶、風險快照、資金狀態與流水查詢。
- 持倉更新、maker/taker fee、referral rebate、realized PnL。
- 資金費結算、強平、保險基金與 ADL queue MVP。
- 快照、事件回放與 recovery/validation 入口。
- Restore smoke API 包含 `GET /api/recovery/restore/account-position-consistency`，可檢查 account / open position 一致性。
- Domain event 發布、outbox retry、DLQ、Kafka event store。
- 輕量 operations metrics endpoint，提供訂單狀態計數、下單延遲、撤單數與成交事件數。
- SSE / WebSocket 推送市場與使用者私有事件，使用者 WebSocket 可 opt-in 啟用 cancel-on-disconnect。

### Polymarket 整合

- Gamma market discovery 與同步進度管理。
- 市場 metadata 入庫與價格刷新。
- CLOB API key create / derive。
- Session Signer init / confirm / list / revoke。
- CLOB order EIP-712 signing 與 L2 HMAC request signing。
- Polymarket 真實下單、內部訂單查詢、單筆同步、取消、reconcile。
- ERC20 collateral allowance 與 ERC1155 conditional token approval status 查詢。
- Polymarket user WebSocket 訂閱 order / trade / settlement lifecycle，並推送到 Kafka topic `polymarket.user.events`。

## 業務模塊

| 模塊 | 主要 package | 職責 |
| --- | --- | --- |
| API 介面層 | `interfaces.web` | REST controller、DTO、validator、exception handler、SSE/WebSocket 設定 |
| Event Consumer | `interfaces.consumer` | 消費 Kafka topic，銜接推送、追蹤或後續業務處理 |
| UseCase | `application.usecase` | 接住 API intent，協調服務完成單一業務動作 |
| Application Service | `application.service` | 編排 repository、domain service、event publisher 與外部資源 |
| Scheduler | `application.scheduler` | 定期觸發 snapshot、funding、reconcile、Polymarket sync |
| Domain Model | `domain.model` | 訂單、帳戶、持倉、流水、Polymarket market/order/session 等核心資料模型 |
| Domain Service | `domain.service` | 撮合規則、訂單簿、Polymarket market/order/session/user WS 邏輯 |
| Repository Contract | `domain.repository` | 領域層資料存取介面與 JPA repository |
| Infrastructure | `infra` | Kafka、Redis、matching engine、Web3j、OkHttp、Spring config |
| Resources | `src/main/resources` | profile 設定、Flyway migration、靜態測試頁 |

## 架構

專案採用分層架構，核心依賴方向由外往內，外層 adapter 依賴 application/domain，domain 不直接依賴 Spring Web 或具體基礎設施。

```text
Client / Script / Frontend
        |
        v
interfaces.web Controller / DTO / Validator
        |
        v
application UseCase / Service / Scheduler
        |
        v
domain Model / Service / Event / Repository Contract
        |
        v
infra Redis / Kafka / MySQL JPA / Matching / HTTP / Web3j
```

交易事件資料流：

```text
POST /api/order/place
  -> PlaceOrderUseCase
  -> RiskService pre-check
  -> OrderService
  -> InMemoryMatchingEngine
  -> Position / Account / Ledger update
  -> EventStore + DomainEventPublisher
  -> Kafka topics
  -> Consumer / PushGateway / Recovery
```

## 下單鏈路

目前內部交易所下單入口是 `POST /api/order/place`，同步完成基本驗證、風控預檢、撮合、帳務與事件發布後回傳 `accepted`。這條鏈路仍是 MVP 實作，production 前建議參考 `todo.md` 補齊持久化撮合、更完整 replay 與更嚴格的帳務一致性。

```text
HTTP POST /api/order/place
  |
  v
OrderController.place()
  - 接收 PlaceOrderRequest
  - Bean Validation 檢查 uid / symbol / side / type / qty / leverage / marginMode
  - 轉成 PlaceOrderCommand
  |
  v
PlaceOrderUseCase.handle()
  - 檢查 command 基本欄位
  - 查 SymbolConfig，解析 Symbol / TimeInForce / MarginMode
  - LIMIT 必須帶 price，MARKET price 為 null
  - 建立 Order 聚合
  |
  v
RiskService.preCheckAndReserve()
  - 檢查 symbol config、槓桿、價格偏離、reduceOnly、可用餘額
  - 計算委託預凍金額
  - 寫入 order reserve ledger
  |
  v
OrderService.processOrder()
  - 將 Order 送入 MatchingEngine
  - 取得 MatchingResult：trades + affectedOrders
  - 對每筆 TradeExecuted 寫入 EventStore 並取得 seq
  - 用 idempotency key 避免重複處理同一成交帳務
  - 更新 Position、position margin、realized PnL、fee、rebate
  - 對所有 affectedOrders 做 reserve reconciliation
  - 回寫訂單狀態到 OrderRepository
  - 更新 MarketDataService 的 trade tape / ticker / kline / depth delta
  - 透過 DomainEventPublisher 發布成交事件到 Kafka
  |
  v
Kafka / Consumer / Push
  - trade.executed：成交事件
  - order.lifecycle：訂單生命週期審計事件
  - event.store.trade：可回放事件
  - TradeEventConsumer 推送 market/user event
```

成功下單不代表一定成交：

- 若 LIMIT 未完全成交且符合 GTC 掛單條件，剩餘量會留在 order book。
- 若 MARKET、IOC、FOK 或 POST_ONLY 條件不成立，訂單可能部分成交、失效或被拒絕。
- 若風控或帳務預檢失敗，請求會在送入撮合前被拒絕。
- 當前 API 回傳 `accepted`，實際最終狀態要查 `GET /api/order/open`、`GET /api/order/all` 或消費事件流。

訂單管理端點：

- `PATCH /api/order/{orderId}` 修改仍在簿內的 LIMIT 掛單，不會主動吃單。Request 可調整 `price`、剩餘 `qty` 與 `clientOrderId`，系統會同步調整剩餘委託預凍。
- `POST /api/order/{orderId}/replace` 先取消原本的 open order，再用提供的 `price`、`qty` 或 `clientOrderId` 建立 replacement order。
- `DELETE /api/order/open?uid=...&symbol=...` 批量取消 open orders 並釋放剩餘委託預凍；不帶 `symbol` 時會取消該使用者所有 open orders。
- `GET /api/order/{orderId}/lifecycle` 查 durable order lifecycle event log；`GET /api/order/{orderId}/projection` 查最新狀態 projection；`POST /api/order/{orderId}/projection/rebuild` 會用 event log 重建 projection。
- `/ws/user/{uid}?cancelOnDisconnect=true&symbol=BTCUSDT` 會為該 user WebSocket 連線 opt-in 啟用 cancel-on-disconnect；不帶 `symbol` 時，斷線會取消該使用者所有 open orders。重連 client 可帶 `resumeConnectionId=<oldSessionId>`，在舊 close event 被處理前把 cancel-on-disconnect 註冊轉移到新 WebSocket session。
- `GET /api/depth/{symbol}` 會回傳完整簿檔 levels、`version` 與 CRC32 `checksum`。`GET /api/market-data/{symbol}/depth-delta` 也會回傳同一條 monotonic depth `version` 與 checksum，供 client 做 snapshot + delta 校驗。

Polymarket 資料流：

```text
Gamma API / CLOB API / User WebSocket
  -> Polymarket domain service
  -> JPA entity / Redis state
  -> Kafka polymarket.user.events
  -> PolymarketUserEventConsumer
  -> Local order tracking / reconcile
```

目錄結構：

```text
src/main/java/com/example/exchange
├── interfaces
│   ├── web          # REST / DTO / Validator / Exception / Push config
│   └── consumer     # Kafka consumer
├── application
│   ├── command      # UseCase command
│   ├── usecase      # 應用流程入口
│   ├── service      # 應用服務
│   ├── scheduler    # 排程任務
│   └── event        # Event publisher abstraction
├── domain
│   ├── model        # Entity / DTO / Enum
│   ├── service      # Matching、Polymarket、order book 等領域服務
│   ├── repository   # Repository contract / JPA repository
│   ├── event        # Domain events
│   └── util         # Signing、JSON、parser utilities
└── infra
    ├── config       # Spring/Kafka/Redis/Web3j/OkHttp config
    ├── kafka        # Kafka adapter
    ├── matching     # In-memory matching engine
    └── redis        # Redis repository implementation
```

## 主要 API

### Exchange

- `POST /api/margin/deposit`
- `POST /api/margin/withdraw`
- `POST /api/margin/transfer`
- `GET /api/margin/account?uid=1`
- `GET /api/margin/ledger?uid=1`
- `GET /api/margin/ledger/replay?uid=1&asset=USDT`
- `GET /api/margin/ledger/replay/compare?uid=1&asset=USDT`
- `GET /api/margin/transfers?uid=1`
- `GET /api/margin/risk?uid=1`
- `POST /api/margin/risk/snapshot?uid=1`
- `POST /api/margin/risk/snapshots`
- `GET /api/margin/risk/snapshot/latest?uid=1`
- `GET /api/margin/risk/snapshots?uid=1&limit=30`
- `GET /api/margin/bonus-credit/campaign-export?campaignId=camp-1&asset=USDT`
- `GET /api/margin/turnover/export?uid=1&symbol=BTCUSDT&strategyId=strategy-1&marketMakerId=mm-1`
- `GET /api/recovery/finance/category-export-batch?date=2026-05-30`
- `GET /api/recovery/finance/ledger-archive-restore-smoke?date=2026-05-30`
- `GET /api/recovery/finance/ledger-archive-replay-validation?fromDate=2026-05-29&toDate=2026-05-30`
- `GET /api/recovery/outbox/domain-state-consistency?limit=50`
- `POST /api/order/place`
- `PATCH /api/order/{orderId}`
- `POST /api/order/{orderId}/replace`
- `DELETE /api/order/{orderId}`
- `DELETE /api/order/open?uid=1&symbol=BTCUSDT`
- `GET /api/order/open?uid=1&symbol=BTCUSDT`
- `GET /api/order/all?uid=1&symbol=BTCUSDT`
- `GET /api/order/{orderId}/lifecycle`
- `GET /api/order/{orderId}/projection`
- `POST /api/order/{orderId}/projection/rebuild`
- `GET /api/order/projections?uid=1&symbol=BTCUSDT`
- `GET /api/depth/{symbol}?depth=10`
- `GET /api/market-data/{symbol}/ticker`
- `GET /api/market-data/{symbol}/trades`
- `GET /api/market-data/{symbol}/klines`
- `GET /api/market-data/{symbol}/depth-delta`
- `GET /api/market-data/{symbol}/stream`
- `GET /api/market-data/user/{uid}/stream`
- `PUT /api/risk/price-oracle`
- `GET /api/risk/price-oracle/{symbol}`
- `POST /api/market-maker/quotes`
- `GET /api/market-maker/quotes/active?limit=50`
- `GET /api/market-maker/quotes/reconciliation?limit=50`
- `GET /api/market-maker/profiles/{marketMakerId}/quotes`
- `GET /api/market-maker/profiles/{marketMakerId}/quotes/{symbol}`
- `POST /api/risk/funding/settle`
- `GET /api/ops/metrics`
- `POST /api/risk/liquidate`
- `GET /api/risk/insurance-fund`
- `GET /api/risk/insurance-fund/movements?asset=USDT&limit=50`
- `GET /api/recovery/reconcile/accounts`
- `POST /api/recovery/reconcile/accounts/report`
- `GET /api/recovery/reconcile/reports?limit=20`
- `GET /api/recovery/reconcile/reports/{reportId}`
- `GET /api/recovery/finance/ledger-archive-delete-guard?date=YYYY-MM-DD`
- `GET /api/risk/adl-queue`
- `GET /api/risk/adl-queue/alerts?minAgeSeconds=900`
- `GET /api/risk/adl-executions?limit=50`
- `GET /api/risk/adl-insurance-reconciliation?asset=USDT`
- `POST /api/recovery/recover/{uid}?fromSeq=0`
- `GET /api/recovery/validate/{uid}`
- `GET /api/recovery/outbox/dlq?limit=50`
- `POST /api/recovery/outbox/dead/{outboxId}/replay`
- `POST /api/recovery/outbox/dead/{outboxId}/compensate`

### Prediction / Polymarket

- `POST /api/prediction/markets/discover`
- `POST /api/prediction/markets/sync`
- `POST /api/prediction/markets/sync-reset`
- `GET /api/prediction/markets/sync-progress`
- `POST /api/prediction/markets/retry/{eventSlug}`
- `POST /api/prediction/markets/price-refresh`
- `GET /api/prediction/markets`
- `POST /api/prediction/clob/api-key/create?nonce=0`
- `GET /api/prediction/clob/api-key/derive?nonce=0`
- `POST /api/prediction/session/init`
- `POST /api/prediction/session/confirm`
- `GET /api/prediction/session/list?userAddress=0x...`
- `POST /api/prediction/session/revoke`
- `POST /api/prediction/session/revoke-all?userAddress=0x...`
- `POST /api/prediction/orders`
- `GET /api/prediction/orders/local`
- `GET /api/prediction/orders/local/{internalOrderId}`
- `POST /api/prediction/orders/local/{internalOrderId}/sync`
- `POST /api/prediction/orders/local/{internalOrderId}/cancel?commandId=...`
- `POST /api/prediction/orders/reconcile`
- `GET /api/prediction/orders/trades`
- `POST /api/prediction/ws/user/start`
- `POST /api/prediction/ws/user/stop`
- `GET /api/prediction/ws/user/status`
- `GET /api/prediction/approve/collateral/allowance`
- `GET /api/prediction/approve/conditional-tokens/status`

## Kafka Topics

`docker-compose.yml` 會建立目前程式使用的 topics：

- `trade.executed`
- `order.lifecycle`
- `event.store.trade`
- `funding.settled`
- `position.liquidated`
- `domain.events`
- `polymarket.user.events`

## 設定

主要設定檔：

- `src/main/resources/application.yml`：預設 profile 與 application name。
- `src/main/resources/application-dev.yml`：本機 MySQL、Redis、Kafka、Polymarket、Web3 設定。
- `src/main/resources/application-prod.yml`：生產 profile 基礎設定。
- `docker-compose.yml`：本機 Kafka、Redis、MySQL、Kafka UI。

Polymarket 的 `private-key`、CLOB `api-key/api-secret/api-passphrase`、relayer key 不應提交到 Git。正式環境應改用環境變數或 secret manager 注入。

若要用 Gmail SMTP 寄送客戶註冊信箱驗證碼，請看 `docs/operations/gmail-smtp.md`。必須使用 Gmail App Password，不要使用帳號登入密碼，並透過環境變數注入。

敏感設定相關環境變數：

- `POLYMARKET_WALLET_PRIVATE_KEY`
- `POLYMARKET_WALLET_FUNDER_ADDRESS`
- `POLYMARKET_WALLET_SIGNATURE_TYPE`
- `POLYMARKET_CLOB_API_KEY`
- `POLYMARKET_CLOB_API_SECRET`
- `POLYMARKET_CLOB_API_PASSPHRASE`
- `POLYMARKET_RELAYER_API_KEY`
- `WEB3_POLYGON_RPC_URL`

交易、資金、admin 類 API 會經過 `security-controls` interceptor。它會套用 rate limit、可選的 IP allowlist 檢查，並輸出 `SECURITY_AUDIT` log。

安全控制相關環境變數：

- `SECURITY_CONTROLS_ENABLED`
- `SECURITY_CONTROLS_AUDIT_ENABLED`
- `SECURITY_CONTROLS_RATE_LIMIT_ENABLED`
- `SECURITY_CONTROLS_REQUESTS_PER_MINUTE`
- `SECURITY_CONTROLS_MAX_TRACKED_KEYS`
- `SECURITY_CONTROLS_IP_ALLOWLIST_ENABLED`
- `SECURITY_CONTROLS_IP_ALLOWLIST`
- `SECURITY_CONTROLS_CLIENT_IP_HEADER`

受保護 API 也支援 `api-auth` authentication / authorization。正式環境預設啟用，支援 SHA-256 hash API key 與 HS256 JWT bearer token。Admin API 需要 `ROLE_ADMIN` 或 `admin` scope；交易 API 需要 trader/user 角色或交易 scope；資金 API 需要 funds/admin 角色或 funds scope。

API auth 相關環境變數：

- `API_AUTH_ENABLED`
- `API_AUTH_API_KEY_ENABLED`
- `API_AUTH_API_KEY_HEADER`
- `API_AUTH_API_KEYS`
- `API_AUTH_JWT_ENABLED`
- `API_AUTH_JWT_HMAC_SECRET`
- `API_AUTH_CLOCK_SKEW_SECONDS`

`API_AUTH_API_KEYS` 格式：

```text
keyId:sha256Hex:ROLE_ADMIN|ROLE_TRADER:admin|trade:write;trader:sha256Hex:ROLE_TRADER:trade:write
```

## 測試

```bash
./mvnw test
```

目前測試涵蓋：

- in-memory matching engine FIFO、post-only、self match prevention。
- 下單後持倉、手續費、流水、市場資料與事件發布。
- mark/index price oracle、risk tiers、funding settlement、liquidation、insurance fund 與 reconciliation。

## 參考文件

- `todo.md`
- `../../src/main/java/com/example/exchange/infra/matching/README_ch.md`
- `../../shells/api-curls/README_ch.md`
