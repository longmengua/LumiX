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
- CLOB API credentials create / derive，不依賴官方 SDK
- Polymarket user WebSocket：接收本錢包 order / trade / settlement lifecycle 更新
- Session Signer init / confirm / list / revoke
- Polymarket CLOB order EIP-712 signing
- Polymarket CLOB L2 HMAC request signing
- 真實 CLOB 下單 API
- ERC20 collateral allowance 查詢
- ERC1155 conditional token approval status 查詢

Markets / sync：

```bash
curl -X POST http://localhost:8080/api/prediction/markets/discover
curl -X POST http://localhost:8080/api/prediction/markets/sync
curl -X POST http://localhost:8080/api/prediction/markets/sync-reset
curl -X POST http://localhost:8080/api/prediction/markets/price-refresh
curl http://localhost:8080/api/prediction/markets/sync-progress
curl http://localhost:8080/api/prediction/markets
```

CLOB credentials：

```bash
# 用 polymarket.wallet.private-key 建立 CLOB API credentials
curl -X POST "http://localhost:8080/api/prediction/clob/api-key/create?nonce=0"

# 如果 nonce 已建立過，用 derive 取回同一組 credentials
curl "http://localhost:8080/api/prediction/clob/api-key/derive?nonce=0"
```

Session / order：

```bash
curl -X POST http://localhost:8080/api/prediction/session/init
curl -X POST http://localhost:8080/api/prediction/session/confirm
curl "http://localhost:8080/api/prediction/session/list?userAddress=0x..."
curl -X POST http://localhost:8080/api/prediction/session/revoke
curl -X POST http://localhost:8080/api/prediction/orders
```

User WebSocket：

```bash
# 狀態查詢
curl http://localhost:8080/api/prediction/ws/user/status

# 啟動後會用 polymarket.clob.api-* 訂閱官方 user channel
curl -X POST http://localhost:8080/api/prediction/ws/user/start

# 停止長連線
curl -X POST http://localhost:8080/api/prediction/ws/user/stop
```

Polymarket user WebSocket 事件會發布到 Kafka topic：

```text
polymarket.user.events
```

目前接收官方 user channel 的 `order` 與 `trade` 類事件；成交從 matched 到 confirmed / failed 的 settlement lifecycle 也會保留在原始 payload 內。這條 channel 使用 CLOB `apiKey / secret / passphrase` 驗證，官方會按 API key 過濾，因此只接收該錢包相關資訊。

目前 WS 流程：

```text
Polymarket user WSS
    │
    │ apiKey / secret / passphrase auth
    │ PING heartbeat every 10s
    ▼
PolymarketUserWebSocketService
    │
    │ parse order / trade payload
    │ keep raw payload for reconciliation
    ▼
Kafka: polymarket.user.events
```

`polymarket.ws.user-market-condition-ids` 可選填 condition id，用來只訂閱特定市場；空陣列代表接收這組 CLOB API key 的全部個人更新。正式環境應讓 WS 服務獨立部署並搭配 consumer 落庫，避免長連線生命週期被 REST app 重啟影響。

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
- 不用官方 SDK 時，後端要自己做兩件事：
  - L1：用 private key 簽 CLOB `ClobAuth` EIP-712，建立或 derive `apiKey / secret / passphrase`。
  - L2：下單時用 `apiSecret` 對 request 做 HMAC header，同時 order payload 仍要 EIP-712 簽名。
- 目前下單簽名優先使用 `polymarket.wallet.private-key`；`sessionId` 保留作為平台內部授權，不直接暴露 private key 到前端。

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
- `polymarket.ws.user-url`
- `polymarket.ws.user-enabled`
- `polymarket.ws.user-market-condition-ids`
- `polymarket.ws.ping-interval-ms`
- `polymarket.ws.reconnect-delay-ms`
- `polymarket.wallet.private-key`
- `polymarket.wallet.funder-address`
- `polymarket.wallet.signature-type`
- `polymarket.trading.buy-markup-rate`
- `polymarket.trading.sell-profit-fee-rate`
- `polymarket.trading.max-slippage-rate`
- `web3.polygon-rpc-url`

Polymarket 下單前的建議設定順序：

1. 填入 `polymarket.wallet.private-key`、`polymarket.wallet.funder-address`、`polymarket.wallet.signature-type: 3`。
2. 呼叫 `POST /api/prediction/clob/api-key/create?nonce=0`。
3. 將回傳的 `apiKey`、`secret`、`passphrase` 填入 `polymarket.clob.*`。
4. 重新啟動服務。
5. 用 `POST /api/prediction/ws/user/start` 啟動私有 order/trade 更新。
6. 用 `POST /api/prediction/orders` 測試下單。

WS 測試建議：

```bash
# 只查狀態，不啟動長連線
./shells/api-curls/polymarket.sh

# 啟動 Polymarket user channel
RUN_USER_WS=1 ./shells/api-curls/polymarket.sh

# 停止 Polymarket user channel
RUN_USER_WS_STOP=1 ./shells/api-curls/polymarket.sh
```

正式環境不要把 key 寫在 yml，應改用 Vault / KMS / Secret Manager / Kubernetes Secret。

---

## 測試指引

以下是目前建議的本機實測順序。先跑只讀與同步類 API，再啟動私有 WS，最後才測真實下單。

### 1. 啟動環境

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

確認基礎編譯與 Spring context：

```bash
./mvnw test
```

### 2. 準備設定

在 `src/main/resources/application-dev.yml` 填好：

- `polymarket.wallet.private-key`
- `polymarket.wallet.funder-address`
- `polymarket.wallet.signature-type: 3`
- `web3.polygon-rpc-url`

第一次還沒有 CLOB credentials 時，先產生：

```bash
RUN_CLOB_AUTH=1 CLOB_AUTH_NONCE=0 ./shells/api-curls/polymarket.sh
```

將回傳的 `apiKey`、`secret`、`passphrase` 填回：

- `polymarket.clob.api-key`
- `polymarket.clob.api-secret`
- `polymarket.clob.api-passphrase`

填完後重啟 Spring Boot。

### 3. 測 market sync

先跑你已驗證過的查詢與 discover：

```bash
curl http://localhost:8080/api/prediction/markets
curl -X POST http://localhost:8080/api/prediction/markets/discover
curl http://localhost:8080/api/prediction/markets/sync-progress
```

再測目前待確認的 sync / price refresh：

```bash
curl -X POST http://localhost:8080/api/prediction/markets/sync
curl -X POST http://localhost:8080/api/prediction/markets/price-refresh
curl http://localhost:8080/api/prediction/markets
```

也可以用整包腳本跑一般檢查：

```bash
./shells/api-curls/polymarket.sh
```

### 4. 測 Polymarket user WS

先查狀態：

```bash
curl http://localhost:8080/api/prediction/ws/user/status
```

啟動 user channel：

```bash
curl -X POST http://localhost:8080/api/prediction/ws/user/start
```

或使用 shell：

```bash
RUN_USER_WS=1 ./shells/api-curls/polymarket.sh
```

啟動後下一筆你的 Polymarket order / trade / settlement lifecycle 更新會被發布到 Kafka：

```text
polymarket.user.events
```

停止 WS：

```bash
curl -X POST http://localhost:8080/api/prediction/ws/user/stop
```

### 5. 測 approval 狀態

這會打 Polygon RPC：

```bash
RUN_APPROVAL=1 OWNER=0x你的funderAddress ./shells/api-curls/polymarket.sh
```

確認 collateral allowance 與 conditional token approval 都符合預期後，再做真實下單。

### 6. 測真實下單

真實下單會送到 Polymarket CLOB。確認 market、金額與錢包設定後才執行：

```bash
RUN_REAL_ORDER=1 \
SESSION_ID=你的sessionId \
MARKET_SLUG=fifwc-mex-rsa-2026-06-11-mex \
DIRECTION=BUY_YES \
USDT_AMOUNT=1 \
ORDER_TYPE=FOK \
./shells/api-curls/polymarket.sh
```

下單後檢查：

- `POST /api/prediction/orders` 的回傳是否成功。
- `GET /api/prediction/ws/user/status` 的 `lastMessageAt` 是否更新。
- Kafka `polymarket.user.events` 是否收到 `order` / `trade` event。
- `GET /api/prediction/markets` 的價格資料是否仍正常。

---

## TODO

### P0：Polymarket 接入完成

- [ ] `prediction_market_info` 補 `neg_risk` 欄位，交易時不再硬編碼 sports 預設值。
- [ ] `prediction_market_info` 補 `no_buy_price`、`no_sell_price`，價格刷新時直接入庫。
- [ ] 下單成功後建立內部 order record，保存 `internalOrderId`、Polymarket order id、market、token、side、price、size、status。
- [x] 接 Polymarket user WebSocket，接收本錢包 order / trade / settlement lifecycle 更新。
- [ ] 將 Polymarket user WebSocket 事件落 DB，更新內部 order / trade 狀態。
- [ ] 建立 `polymarket.user.events` consumer，處理 order accepted / canceled / matched / confirmed / failed。
- [ ] 建立 WS event idempotency key，避免重連或 replay 造成重複成交。
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
