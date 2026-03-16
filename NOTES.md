# 交易所 / 合約撮合系統專案狀態分析

從目前的程式結構可以看出，這是一個 **交易所 / 合約撮合系統的雛形**。  
它已經包含了多個交易系統核心組件，而不是單純的 CRUD 專案。

目前系統涉及的核心能力包括：

- 下單
- 撮合
- 持倉更新
- 保證金劃轉
- 訂單簿查詢
- 事件發布
- 快照恢復

整體狀態可以概括為：

> **核心骨架已經搭建完成，主流程可以運作，但距離真正可用的交易所系統仍有一段距離。**

本文將系統整理為兩個部分：

1. **已完成功能**
2. **未完成 / 僅有骨架的部分**

---

# 已完成的功能

## 1. Spring Boot 專案架構

專案已建立完整的 **Spring Boot 分層架構**：

```

interfaces   -> REST API / DTO / Interceptor / Exception
application  -> UseCase / Command / Service / Scheduler
domain       -> Model / Repository / Domain Service / Event
infra        -> Redis / Kafka / Matching Engine / Config

````

整體設計風格偏向：

**DDD + Clean Architecture**

---

## 2. Web API 與系統入口

目前系統已經提供多個對外 API。

### Order API

```http
POST /api/order/place
````

下單

```http
GET /api/order/open
```

查詢未完成訂單

```http
GET /api/order/all
```

查詢所有訂單（目前實作有限）

---

### Margin API

```http
POST /api/margin/transfer
```

Cross / Isolated 保證金劃轉

---

### Market API

```http
GET /api/depth/{symbol}
```

查詢訂單簿深度

---

### Recovery API

```http
POST /api/recovery/recover/{uid}
```

手動執行快照恢復

---

### 其他已完成基礎能力

* 全域 Exception Handler
* Request Logging Interceptor
* DTO validation 基礎框架

因此：

> 從 HTTP API 層面來看，系統已經是一個 **可操作的交易系統雛形**。

---

## 3. 撮合引擎雛形

撮合系統是目前專案最核心的部分。

主要組件：

* `MatchingEngine`
* `InMemoryMatchingEngine`
* `OrderBook`
* `OrderBookSnapshot`
* `TopOfBook`

---

### 撮合引擎已具備能力

* 依 `symbol` 分開維護 OrderBook
* Bid / Ask 雙邊訂單簿
* 支援 `BUY / SELL`
* 支援 `LIMIT / MARKET`
* 成交事件產生
* Top Of Book 查詢
* Depth Snapshot
* Cancel Order
* MARKET 單未成交部分轉成類似 **MTL**

代表：

> **撮合主循環已存在，而不是空殼。**

---

## 4. 訂單流程主線已串接

下單流程：

```
OrderController
      ↓
PlaceOrderUseCase
      ↓
OrderService
      ↓
MatchingEngine
      ↓
EventStore
      ↓
PositionRepository
      ↓
DomainEventPublisher
      ↓
OrderRepository
```

---

### 下單後系統行為

系統目前會：

1. 建立 `Order`
2. 送入撮合引擎
3. 生成 `TradeExecuted` 事件
4. 寫入 EventStore
5. 更新 Position
6. 發布 Kafka 事件
7. 回寫 OrderRepository

這條鏈已經把：

* 訂單
* 撮合
* 事件
* 持倉

串成一條完整的業務流程。

---

## 5. Position 持倉模型

`Position` 已經支援：

* `qty > 0` → 多倉
* `qty < 0` → 空倉
* `entryPrice`
* `marginMode`
* `leverage`

核心方法：

```java
applyTrade(tradeQty, tradePrice)
```

已處理：

* 新開倉
* 加倉
* 減倉
* 平倉歸零

因此：

> 系統已經具備 **持倉演算能力**。

---

## 6. Account 與 Margin 基本能力

主要類別：

* `Account`
* `MarginService`

---

### 已具備能力

* Cross Balance
* Cross Available
* Isolated Margin
* `deposit`
* `withdraw`
* `moveToIsolated`
* `moveFromIsolated`

UseCase：

```
TransferMarginUseCase
```

Controller：

```
MarginController
```

說明：

> 系統已具備 **Cross / Isolated 資金池概念**

---

## 7. Redis 持久化

目前 Redis 已接入以下 repository：

* `RedisAccountRepository`
* `RedisOrderRepository`
* `RedisPositionRepository`
* `RedisSnapshotRepository`

---

### RedisOrderRepository 已實現

* 單筆訂單保存
* User order index
* Open order 查詢
* All order 查詢
* dangling id 清理

Redis 層目前是 Infra 中較完整的一部分。

---

## 8. Kafka 事件流

目前 Kafka 已包含：

* `KafkaDomainEventPublisher`
* `KafkaEventStore`
* `TradeEventConsumer`
* `KafkaConfig`

---

### 已具備能力

* 發送成交事件
* Event Store Topic
* Topic 消費入口

整體架構開始朝：

> **Event-driven Exchange Backend**

方向發展。

---

## 9. Snapshot 與 Recovery

目前已有：

* `Snapshot`
* `SnapshotRepository`
* `SnapshotScheduler`
* `RecoveryService`
* `SnapshotRecoverUseCase`
* `RecoveryController`

---

### 已實作功能

* 定時產生 Snapshot
* 保存 Account / Position 摘要
* UID 快照恢復
* Event Replay 預留

說明：

> 已開始設計 **事件回放 + 狀態恢復機制**

---

## 10. Scheduler 基礎框架

目前存在兩個排程器：

* `FundingRateScheduler`
* `SnapshotScheduler`

雖然 Funding Rate 還未實作，但架構已預留。

---

# 未完成 / 只有骨架的部分

以下是目前專案最大的缺口。

---

# 1. 風控系統

目前風控幾乎未實作。

應該包含：

* 用戶交易權限
* 交易對可交易狀態
* 最小下單量
* 最小名義價值
* price tick
* qty step
* leverage 檢查
* 可用餘額
* Initial Margin 試算
* 手續費凍結
* 倉位風險檢查
* 爆倉門檻

目前狀態：

> **API 打進來就嘗試下單**

---

# 2. 手續費系統

目前尚未實作：

* maker fee
* taker fee
* VIP rate
* fee freeze
* fee deduction
* 手續費結算

目前成交只影響：

```
qty
price
```

沒有完整資產流。

---

# 3. 保證金系統

雖然有 `Account` 與 `MarginMode`，但仍不完整。

缺少：

* Initial Margin
* Maintenance Margin
* Cross Risk
* Isolated Risk
* 凍結 / 解凍
* Unrealized PnL
* Realized PnL
* liquidation threshold

---

# 4. 強平系統

目前只有骨架：

* `LiquidateCommand`
* `LiquidateUseCase`
* `PositionLiquidated`

未實作：

* 維持保證金率
* 破產價
* 清算單
* 保險基金
* ADL
* 強平事件

---

# 5. Funding Rate

`FundingRateScheduler` 目前為 TODO。

缺少：

* funding rate calculation
* index price
* mark price
* 8h settlement
* 多空轉移
* funding settlement event

---

# 6. 訂單簿仍為簡化版

目前 `OrderBook` 仍缺：

* 嚴格 FIFO
* Price-time priority
* Level queue
* SMP
* Partial fill model
* match sequence
* maker/taker flag
* reduce-only
* post-only
* IOC / FOK
* cancel-replace
* batch order

且目前為：

```
in-memory matching engine
```

不是可擴展撮合核心。

---

# 7. 訂單查詢不一致

Repository 實作與 API 契約不一致。

例如：

```
findOpenOrders(uid, symbol)
```

如果 `symbol = null`

目前返回：

```
empty
```

但 API 文件表示：

```
應返回 uid 的所有交易對訂單
```

---

# 8. Snapshot Recovery 不完整

目前只做：

* 找最新 snapshot
* 重建部分 position
* 預留 replay

未完成：

* account restore
* position restore
* order restore
* event replay
* deduplication
* consistency check

---

# 9. Event Store 為 Demo 級

目前使用：

```
AtomicLong seq
```

問題：

* 非持久
* 重啟失效
* 多節點不一致
* 無 shard
* lastSeq(uid) 不可靠

---

# 10. 資產閉環未完成

交易所需要能回答：

* 可用餘額
* 凍結資金
* 使用保證金
* unrealized pnl
* realized pnl
* 手續費
* funding
* liquidation loss

目前尚未形成完整資產帳務系統。

---

# 11. Validation 不完整

目前 validation 僅為基礎版。

缺少：

* `@ValidSymbol`
* enum validation
* LIMIT / MARKET 條件驗證
* depth max limit
* `@Positive`
* `@NotNull`

---

# 12. 運維與監控

目前只有 request logging。

缺少：

* actuator
* metrics
* tracing
* structured logging
* audit log
* DLQ
* retry
* idempotency

---

# 專案成熟度評估

## 已完成層級

目前是一個：

> **Exchange Backend Prototype / MVP Skeleton**

具備：

* API
* Matching
* OrderBook
* Position
* Redis
* Kafka
* Snapshot

---

## 尚缺核心能力

* 風控
* 保證金
* 手續費
* 強平
* 資金費
* Event Store
* 完整恢復
* 交易規則

---

# 專案類型判斷

此專案最接近：

> **永續合約交易所 Backend Prototype**

因為已包含：

* Cross / Isolated
* Leverage
* Position
* Funding
* Liquidation
* Event Replay
* Snapshot Recovery

---

# 總結

## 已完成

* Spring Boot 架構
* REST API
* 基本撮合
* OrderBook
* Position
* Margin transfer
* Redis
* Kafka
* Snapshot
* Scheduler

---

## 未完成

* 風控
* 手續費
* 保證金
* 強平
* Funding
* Event Store
* Recovery
* Validation
* Observability

---

如果需要，我可以再幫你把這份文件升級成 **完整技術審查報告版 (Tech Review / Architecture Review)**，會包含：

* 系統架構圖
* Order Flow 圖
* Matching Flow
* Asset Flow
* 高風險模組分析
* 建議 Roadmap

```
```
