# Java21-Exchange Demo

此專案是一個基於 **Java 21 + Spring Boot 3.5** 的極簡版「合約交易系統 Demo」，模擬幣安合約核心流程：
- 撮合引擎 (Order Matching)
- 槓桿 / 保證金
- 持倉管理
- 快照 / 強平
- API 提供下單、查詢、訂單簿

---

## 🏛️ 資料夾架構

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

---

## 🧠 核心功能

### 1. 撮合引擎 (Matching Engine)
- `PriorityQueue` 維護 **買單簿 (bids)**、**賣單簿 (asks)**
- 價格優先 / 時間優先
- 支援：
    - **LIMIT**：掛單 / 吃單
    - **MARKET**：不足量剩餘轉 Maker 掛簿
    - **POST_ONLY / REDUCE_ONLY / IOC / FOK**
- 產生事件 `TradeExecuted`

### 2. 槓桿與保證金
- `Position` 模型追蹤倉位數量、均價、模式 (Cross/Isolated)、槓桿
- 撮合層不處理槓桿，由 **Risk Service** 檢查
    - 下單前：是否有足夠保證金
    - 成交後：更新持倉、保證金

### 3. 快照與恢復
- **SnapshotScheduler** 定期保存狀態
- **SnapshotRecoverUseCase** 可依 UID 恢復帳戶狀態

### 4. 強平 (Liquidation)
- **Liquidation Service**：保證金不足觸發
- 部分平倉 → 全平 → ADL
- 保險基金兜底

### 5. 標記價與資金費
- **Index Price**：多交易所聚合、異常值剔除
- **Mark Price**：用於觸發單/風控/強平
- **Funding**：每 8 小時計算結算，盈虧在多空之間轉移

### 6. 保險基金 & ADL
- 保險基金：強平盈餘注入、虧損支出
- ADL：保證金不足且保險基金不夠時，按槓桿/收益排序減倉

### 7. 費用與返佣
- Maker/Taker 分級費率
- 推薦人返佣、平台幣抵扣

### 8. 報表 & 風控
- ClickHouse 報表：交易量、手續費、PnL、VIP 分級
- 管理後台：強平成功率、ADL 次數、保險基金走向、審計日誌

---

## 🏗️ 系統架構

- **Gateway**：API 鑑權、限流
- **Matching**：單簿單執行緒/Disruptor
- **Risk-Margin**：保證金/維持率/風控
- **Wallet-Ledger**：雙式簿記，凍結與結算
- **Position-PnL**：未實現/已實現損益
- **Funding**：資金費率計算與結算
- **Liquidation**：部分/全平/ADL
- **Insurance-Fund**：入/出帳
- **Market-Data**：深度、K 線、行情推送
- **Reporting-OLAP**：ClickHouse 聚合報表
- **Admin-Console**：配置、風控、報表、人工操作
- **Audit-Log**：審計、合規留痕

---

## 🔄 資料流

1. 下單 → 風控預檢 → 撮合 → 事件 `TradeExecuted`
2. 撮合 → Position 更新 → Ledger 記賬 → 報表入庫
3. 標記價更新 → 觸發條件單 / 強平監控
4. 資金費定時計算 → 多空轉移
5. 強平失敗 → 保險基金 → ADL

---

## 📊 資料庫設計

### OLTP (MySQL)
- `order`：委託生命週期
- `trade`：成交記錄
- `position`：持倉
- `funding_history` / `funding_settlement`
- `wallet_ledger`：資產流水
- `insurance_fund_ledger`

### OLAP (ClickHouse)
- `trades_all`：成交寬表
- `positions_snapshots`
- `funding_settled`
- `fees_daily`
- `volume_agg_5m/1h/1d`

---

## 📈 報表示例 (ClickHouse)

**交易量 & 費用**
```sql
SELECT toDate(event_time) AS d, symbol,
       sum(notional) AS vol_usdt,
       sumIf(fee, maker=1) AS maker_fee,
       sumIf(fee, maker=0) AS taker_fee
FROM trades_all
WHERE event_time >= today()-30
GROUP BY d, symbol
ORDER BY d, symbol;
```