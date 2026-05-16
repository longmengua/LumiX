# Java21 Match Hub

Java 21 + Spring Boot 3.5 的交易所核心雛形，目標是逐步演進成可承載高流量的交易與預測市場整合平台。

目前專案包含兩條主線：

- 內部交易所核心：下單、撮合、訂單簿、保證金、持倉、快照、事件發布。
- Polymarket 整合：Gamma market discovery/sync、價格刷新、Session Signer、CLOB 下單、allowance/approval 查詢。

這不是單純教學 Demo。現在的程式碼已經有 DDD 分層、Redis/Kafka/MySQL 基礎設施、Polymarket CLOB 簽名與交易流程雛形；但距離「交易所高流量大項目」還需要補齊撮合性能、帳務一致性、風控、可觀測性、壓測與安全邊界。

---

## 專案分析

### 目前做得好的部分

- 分層清楚：`interfaces`、`application`、`domain`、`infra` 邊界明確，後續拆服務或替換基礎設施比較容易。
- 領域模型已成形：`Order`、`Position`、`Account`、`PredictionMarketInfo`、`PredictionSessionRecord` 等核心物件已存在。
- 撮合引擎有可跑 MVP：支援 LIMIT / MARKET、GTC / IOC / FOK、深度快照、top of book。
- 事件流雛形已建立：`TradeExecuted`、`SnapshotCreated`、Kafka publisher/store 已具備。
- Polymarket 接入方向正確：使用 Deposit Wallet + Session Signer，後端 session signer 只負責簽 CLOB order，不持有使用者資產。
- 本機開發環境完整：Docker Compose 提供 MySQL、Redis、Kafka。

### 目前主要風險

- 撮合核心仍是 memory MVP：目前不是生產級 sequencer / event-sourced matching core，價格時間優先與取消索引也還需要強化。
- 帳務系統還不完整：下單成功後尚未完整落內部 order record、ledger、reconciliation。
- Polymarket market metadata 還有缺欄位：例如 `neg_risk`、NO 買賣價格建議入庫，避免交易時即時計算造成錯誤或不一致。
- 高流量能力尚未驗證：缺少壓測、延遲指標、Kafka topic 規劃、DB index review、Redis cache 策略。
- 安全邊界需要補強：API key/private key 管理、session signer lifecycle、rate limit、審計日誌、風控限額都還需要產品級設計。

---

## 技術棧

- Java 21
- Spring Boot 3.5.6
- Spring Web / Validation
- Spring Data JPA + MySQL 8.4
- Redis 7.4
- Kafka 7.6 KRaft single node
- OkHttp
- Web3j
- Lombok
- Maven

---

## 架構分層

```text
src/main/java/com/example/exchange
├── interfaces
│   ├── web          # REST Controller / DTO / Validator / Exception
│   └── consumer     # Kafka / MQ consumer
├── application
│   ├── command      # UseCase command
│   ├── usecase      # 應用流程入口
│   ├── service      # 應用服務
│   ├── scheduler    # 排程任務
│   └── event        # 應用事件發布
├── domain
│   ├── model        # Entity / DTO / Enum
│   ├── service      # 領域服務，例如 MatchingEngine、Polymarket service
│   ├── repository   # Repository abstraction / JPA repository
│   ├── event        # Domain events
│   └── util         # Polymarket signing / parsing utils
└── infra
    ├── config       # Spring / Kafka / Redis / Web3j / OkHttp config
    ├── kafka        # Kafka adapter
    ├── matching     # In-memory matching implementation
    └── redis        # Redis repository implementation
```

---

## 核心功能

### 內部交易所核心

- 下單 API：`POST /api/order/place`
- 查詢掛單：`GET /api/order/open`
- 查詢全部訂單：`GET /api/order/all`
- 訂單簿深度：`GET /api/depth/{symbol}?depth=10`
- 保證金轉入/轉出
- 持倉與保證金更新
- 快照排程與恢復
- Kafka domain event publish/store

### 撮合引擎

目前實作：`InMemoryMatchingEngine`

- 多 symbol order book
- LIMIT / MARKET
- GTC / IOC / FOK
- 部分成交與剩餘量處理
- book snapshot / top of book

目前限制：

- 尚未完整生產化價格時間優先。
- 尚未完成 Self Match Prevention。
- 尚未完成 POST_ONLY / REDUCE_ONLY。
- 尚未完成 maker/taker fee。
- 尚未使用單 symbol 單執行緒 sequencer / Disruptor 類模型。

### Polymarket 整合

目前已有：

- Gamma API market discovery
- sync key resume / reset / retry
- 5 秒價格刷新
- Bitmart UI 風格 market response
- Session Signer init / confirm / list / revoke
- Polymarket CLOB order signing
- 真實 CLOB 下單 API
- ERC20 collateral allowance 查詢
- ERC1155 conditional token approval status 查詢

主要 API：

```bash
curl -X POST http://localhost:8080/api/prediction/markets/discover
curl -X POST http://localhost:8080/api/prediction/markets/sync
curl -X POST http://localhost:8080/api/prediction/markets/sync-reset
curl -X POST http://localhost:8080/api/prediction/markets/price-refresh
curl http://localhost:8080/api/prediction/markets/sync-progress
curl http://localhost:8080/api/prediction/markets
```

Session / order：

```bash
curl -X POST http://localhost:8080/api/prediction/session/init
curl -X POST http://localhost:8080/api/prediction/session/confirm
curl "http://localhost:8080/api/prediction/session/list?userAddress=0x..."
curl -X POST http://localhost:8080/api/prediction/session/revoke
curl -X POST http://localhost:8080/api/prediction/orders
```

---

## Polymarket 交易模型

```text
User MetaMask
    │
    │ approve / login / ownership
    ▼
Deposit Wallet
    │
    │ holds assets / allowance owner
    ▼
Session Signer
    │
    │ signs CLOB order only
    ▼
Polymarket CLOB
```

設計原則：

- Deposit Wallet 是資產 owner。
- Session Signer 只負責簽 CLOB order。
- 後端不代送 approve transaction。
- CLOB Trading API 是核心下單通道。
- Relayer API 不應取代 CLOB 下單。

---

## 本機啟動

啟動基礎設施：

```bash
docker compose up -d
```

啟動 Spring Boot：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

執行測試：

```bash
./mvnw test
```

本機服務預設：

- App: `http://localhost:8080`
- MySQL: `localhost:3306/appdb`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`

---

## 設定

開發環境設定在 `src/main/resources/application-dev.yml`。

重要設定：

- `polymarket.gamma.base-url`
- `polymarket.clob.base-url`
- `polymarket.clob.api-key`
- `polymarket.clob.api-secret`
- `polymarket.clob.api-passphrase`
- `polymarket.wallet.funder-address`
- `polymarket.wallet.signature-type`
- `polymarket.trading.buy-markup-rate`
- `polymarket.trading.sell-profit-fee-rate`
- `polymarket.trading.max-slippage-rate`
- `web3.polygon-rpc-url`

正式環境不要把 key 寫在 yml，應改用 Vault / KMS / Secret Manager / Kubernetes Secret。

---

## TODO

### P0：Polymarket 接入完成

- [ ] `prediction_market_info` 補 `neg_risk` 欄位，交易時不再硬編碼 sports 預設值。
- [ ] `prediction_market_info` 補 `no_buy_price`、`no_sell_price`，價格刷新時直接入庫。
- [ ] 下單成功後建立內部 order record，保存 `internalOrderId`、Polymarket order id、market、token、side、price、size、status。
- [ ] 建立 reconciliation job，定期對 Polymarket CLOB 訂單狀態與內部訂單狀態。
- [ ] 補 CLOB order 查詢、取消訂單、成交回報同步。
- [ ] Session Signer 加上過期時間、最大下單額、每日限額、撤銷審計。
- [ ] Approval / allowance 結果加入 cache 與過期策略，避免高頻 RPC 查詢。
- [ ] Polymarket API error code 正規化，避免 controller 直接回傳原始 exception message。

### P1：交易所級帳務與風控

- [ ] 補 Wallet-Ledger 雙式簿記模型，所有資金變化必須可追溯。
- [ ] 下單前風控預檢：餘額、保證金、槓桿、最大倉位、最大下單金額、價格偏離。
- [ ] 成交後更新 position、realized PnL、fee、rebate、ledger。
- [ ] 補 maker/taker fee tier、VIP 等級、推薦返佣。
- [ ] 補 liquidation engine：部分平倉、全平、保險基金、ADL。
- [ ] 補 funding rate 計算與結算流水。
- [ ] 補 symbol config：tick size、lot size、min notional、risk tier、fee config。

### P1：高流量撮合與行情

- [ ] 將 memory matching engine 演進為單 symbol 單執行緒 sequencer。
- [ ] 補價格時間優先的嚴格排序與穩定 order id 索引。
- [ ] 補 cancel / amend 的 O(1) 或近 O(1) 查找路徑。
- [ ] 補 Self Match Prevention。
- [ ] 補 POST_ONLY / REDUCE_ONLY 完整語義。
- [ ] Kafka topic 按事件類型與 symbol partition 規劃，確保同 symbol 順序。
- [ ] Market data service 輸出 depth delta、ticker、trade tape、kline。
- [ ] WebSocket gateway 支援訂單簿與使用者私有推送。

### P2：可靠性與資料恢復

- [ ] Snapshot + event replay 完整化，支援指定 offset 恢復。
- [ ] 所有關鍵事件加 idempotency key。
- [ ] 補 outbox pattern，避免 DB commit 成功但 Kafka publish 失敗。
- [ ] 補 DLQ、retry、poison message 處理。
- [ ] 補資料校驗 job：order/trade/position/ledger 對帳。
- [ ] 補 Flyway 或 Liquibase 管理 schema migration，移除正式環境 `ddl-auto=update`。

### P2：觀測、壓測與維運

- [ ] Actuator metrics 接 Prometheus / Grafana。
- [ ] 補 trace id / request id / order id 全鏈路 logging。
- [ ] 補核心指標：下單 QPS、撮合延遲、Kafka lag、DB latency、Redis latency、CLOB latency。
- [ ] 建立 Gatling / k6 壓測場景：下單、撤單、行情查詢、Polymarket 同步。
- [ ] 建立 SLO：下單 p99、撮合 p99、行情推送延遲、錯誤率。
- [ ] 補 runbook：Kafka lag、DB 慢查詢、Polymarket API 限流、RPC 失敗。

### P2：安全與合規

- [ ] API key、CLOB credentials、session private key 改用加密存放。
- [ ] Session private key 加密 at rest，並限制讀取權限。
- [ ] 補 API auth、rate limit、IP allowlist、device/session 管理。
- [ ] 補使用者操作審計、管理員操作審計。
- [ ] 補風控黑名單、異常交易偵測、可疑下單頻率檢查。
- [ ] 補敏感資料遮罩與 log sanitization。

---

## 相關文件

- `docs/Architecture.md`
- `docs/DataFlow.md`
- `docs/TimeFlow.md`
- `polymarket.md`
- `src/main/java/com/example/exchange/infra/matching/README.md`
