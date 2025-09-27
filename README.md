# Java21-Exchange Demo

此專案是一個基於 **Java 21 + Spring Boot 3.5** 的極簡版「合約交易系統 Demo」，模擬幣安合約核心流程：
- 撮合引擎 (Order Matching)
- 槓桿 / 保證金
- 持倉管理
- 快照 / 強平
- API 提供下單、查詢、訂單簿

## 🏛️ 系統架構

```
+--------------------------+
|      interfaces          | <- I/O 邊界層
|                          |    - Web (REST API Controller)
|                          |    - Consumer (Kafka / MQ)
|                          |    - DTO / Validator / Exception Handler
+--------------------------+
|      application         | <- 應用層 (Use Case 驅動流程)
|                          |    - Command / UseCase
|                          |    - Application Service
|                          |    - Scheduler (排程)
|                          |    - Event (應用事件)
+--------------------------+
|      domain              | <- 核心業務邏輯 (不依賴任何技術)
|                          |    - Model (Entity, Value Object, Aggregate)
|                          |    - Domain Event
|                          |    - Repository (抽象介面)
|                          |    - Service (例如 MatchingEngine)
+--------------------------+
|      infra               | <- 技術細節 (Infrastructure)
|                          |    - Redis Repository 實作
|                          |    - Kafka Adapter
|                          |    - Config (Spring Beans, Redis, Kafka)
|                          |    - InMemoryMatchingEngine (撮合引擎實作)
+--------------------------+

```

## 🗂️ 資料夾結構

```
com.example
├─ application
│  ├─ command
│  │  ├─ PlaceOrderCommand.java
│  │  ├─ TransferMarginCommand.java
│  │  ├─ SnapshotRecoverCommand.java
│  │  └─ LiquidateCommand.java
│  ├─ event
│  │  ├─ DomainEventPublisher.java
│  │  └─ handlers
│  │     └─ PositionLiquidatedHandler.java
│  ├─ scheduler
│  │  ├─ FundingRateScheduler.java
│  │  └─ SnapshotScheduler.java
│  ├─ service
│  │  ├─ OrderService.java
│  │  ├─ MarginService.java
│  │  └─ RecoveryService.java
│  └─ usecase
│     ├─ PlaceOrderUseCase.java
│     ├─ TransferMarginUseCase.java
│     ├─ SnapshotRecoverUseCase.java
│     └─ LiquidateUseCase.java
│
├─ domain
│  ├─ event
│  │  ├─ PositionLiquidated.java
│  │  ├─ SnapshotCreated.java
│  │  └─ TradeExecuted.java
│  ├─ model
│  │  ├─ Account.java
│  │  ├─ MarginMode.java
│  │  ├─ Order.java
│  │  ├─ OrderSide.java
│  │  ├─ OrderType.java
│  │  ├─ Position.java
│  │  ├─ Symbol.java
│  │  └─ Snapshot.java
│  └─ repository
│     ├─ AccountRepository.java
│     ├─ OrderRepository.java
│     ├─ PositionRepository.java
│     ├─ EventStore.java
│     └─ SnapshotRepository.java
│
├─ infra
│  ├─ config
│  │  ├─ KafkaConfig.java
│  │  └─ RedisConfig.java
│  ├─ kafka
│  │  ├─ KafkaDomainEventPublisher.java
│  │  └─ KafkaEventStore.java
│  └─ redis
│     ├─ RedisAccountRepository.java
│     ├─ RedisOrderRepository.java
│     ├─ RedisPositionRepository.java
│     └─ RedisSnapshotRepository.java
│
└─ interfaces
   ├─ consumer
   │  └─ TradeEventConsumer.java
   └─ web
      ├─ controller
      │  ├─ OrderController.java
      │  ├─ MarginController.java
      │  └─ RecoveryController.java
      ├─ dto
      │  ├─ PlaceOrderRequest.java
      │  ├─ TransferRequest.java
      │  └─ ApiResponse.java
      ├─ exception
      │  ├─ GlobalExceptionHandler.java
      │  └─ BizException.java
      ├─ interceptor
      │  └─ RequestLoggingInterceptor.java
      └─ validator
```

## 🧠 核心功能

### 1. 撮合引擎 (Matching Engine)
- 使用 `PriorityQueue` 維護 **買單簿 (bids)**、**賣單簿 (asks)**
- 價格優先 / 時間優先
- 支援：
    - **LIMIT**：掛單 / 吃單
    - **MARKET**：先吃不到就轉為 Maker 掛簿
- 產生事件 `TradeExecuted`

### 2. 槓桿與保證金
- `Position` 模型追蹤：
    - 倉位數量 (qty)
    - 進場均價 (entryPrice)
    - 模式 (Cross / Isolated)
    - 槓桿 (leverage)
- 撮合層 **不處理槓桿**，槓桿由風控層檢查：
    - 下單前 → 是否有足夠保證金
    - 成交後 → 更新持倉、保證金

### 3. 快照與恢復
- **SnapshotScheduler** 定期保存狀態
- **SnapshotRecoverUseCase** 可依 UID 恢復帳戶狀態

### 4. 強平 (Liquidation)
- **LiquidateCommand / UseCase**：當保證金不足時觸發強平訂單，丟回撮合引擎
