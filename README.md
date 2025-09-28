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

---

## 🏛️ 系統架構圖（High-Level System Architecture）

```
                        ┌─────────────────────────────────────┐
                        │               Clients               │
                        │  Web / iOS / Android / Market Maker │
                        └───────────────┬─────────────────────┘
                                        │ HTTPS / WebSocket
                               ┌────────▼────────┐
                               │   API Gateway   │  (AuthN/Z, Rate-limit, Routing)
                               └───────┬─────────┘
       ┌────────────────────────────────┼────────────────────────────────┐
       │                                │                                 │
┌──────▼───────┐                 ┌──────▼───────┐                  ┌──────▼────────┐
│   Auth/KYC   │                 │   Order App  │                  │  Admin Console│
│(JWT, API Key │                 │(REST/WS, OCO,│                  │(Ops, Config,  │
│  Risk Score) │                 │ Trigger Mgmt)│                  │  Audit UI)    │
└──────┬───────┘                 └──────┬───────┘                  └──────┬────────┘
       │                                │                                   │
       │                                │ place/cancel/query                │
       │                       ┌────────▼─────────┐                         │
       │                       │   Risk-Margin    │  (pre-check, limits)    │
       │                       └────────┬─────────┘                         │
       │ reserve/release funds           │ ok/reject                        │
┌──────▼────────┐                 ┌──────▼────────┐                  ┌──────▼──────────┐
│ Wallet-Ledger │                 │  Matching Core│  (per-symbol     │  Symbol-Config  │
│(double-entry  │<──reserve/settle│ (sequencer,   │   orderbook)     │(tick, lot, fees,│
│  accounting)  │                 │  price/time   │                  │  risk tiers...) │
└──────┬────────┘                 │  priority)    │                  └──────┬──────────┘
       │   trades, fills          └──────┬────────┘                         │
       │   fees, rebates                 │ trades, book deltas               │
┌──────▼─────────┐                 ┌─────▼──────────┐                 ┌──────▼───────────┐
│ Position & PnL │<────trades──────│ Market-Data    │──WS──> Clients  │ Price-Feed/Index │
│(avg, upnl/rpnl │                 │(depth, ticker, │                 │(Index, MarkPrice │
│ funding, stats)│                 │ kline, mark)   │                 │  outlier filter) │
└──────┬─────────┘                 └────────────────┘                 └──────┬───────────┘
       │  mark price, funding rate updates                                 │ index/mark updates
       │                                                                    │
       │                                                                    │
       │   ┌────────────────────────────────────────────────────────────────▼────────────┐
       │   │                                     Kafka (Events)                           │
       │   │ order.*, trade.executed, position.changed, mark_price.updated, funding.*,    │
       │   │ liquidation.*, wallet.*, snapshot.*                                          │
       │   └───────────────────────────────┬──────────────────────────────────────────────┘
       │                                   │
┌──────▼───────────┐                ┌──────▼──────────┐                 ┌────────▼─────────┐
│ Reporting-OLAP   │<== stream ==>  │   ETL/Replay    │  ==> S3/Blob    │ Snapshot Service │
│ (ClickHouse)     │                │(Kafka Consumer, │  archive,        │ (make/restore)   │
│ BI/Reports/Alerts│                │  CDC, validators│  reprocess)      └────────┬─────────┘
└──────┬───────────┘                └─────────────────┘                         │
       │ SQL (ad hoc / APIs)                                                   │ restore from
       │                                                                        │ snapshot + events
┌──────▼───────────┐     ┌───────────────┐     ┌───────────────┐         ┌──────▼───────────┐
│ MySQL (OLTP)     │     │ Redis (cache) │     │ Timeseries/TS │(opt.)   │  Object Storage  │
│ orders, trades,  │     │ sessions,     │     │ (metrics)     │         │  (S3, snapshots) │
│ positions, ledger│     │ rate limits   │     │               │         └──────────────────┘
└──────────────────┘     └───────────────┘     └───────────────┘
```

---

## 🏛️ DFD（Data Flow Diagram）

```
                 ┌───────────────────┐
                 │   Market Makers   │
                 └─────────┬─────────┘
                           │ Quotes / Orders
                           │
┌──────────────┐    ┌──────▼────────┐    ┌───────────────────┐
│   Traders    │───▶│ Perpetual     │◀──▶│ Market Data Feeds │
│(Web/App/API) │    │ Exchange      │    │ (Exchanges, Oracles)
└──────┬───────┘    │ (System)      │    └───────────────────┘
       │            └──────┬────────┘
       │                   │
       │   Reports/API     │ Events / Data
       │                   │
 ┌─────▼───────────┐    ┌──▼─────────────┐   ┌──────────────────┐
 │ Admin/Compliance│    │ Datastores     │   │  Object Storage  │
 │ (Ops/KYC/Audit) │    │ (MySQL, CH,    │   │  (Snapshots)     │
 └─────────────────┘    │  Kafka, Redis) │   └──────────────────┘
                        └────────────────┘
```

## 事件時序圖：撮合 → 報表（A/B/C 三場景）

```
Actors / Lanes:
[Client]  使用者/做市商（REST/WS）
[GW]      API Gateway（AuthN/Z、限流）
[ORD]     Order App（下/撤/查、觸發單、策略單）
[RISK]    Risk-Margin（預檢、限額、MM/IM）
[WAL]     Wallet-Ledger（雙式簿記，reserve/settle）
[ME]      Matching Engine（每 symbol 單簿序列器）
[POS]     Position & PnL（均價、UPnL/RPnL、費/返）
[MD]      Market-Data（book/aggTrade/mark WS）
[PX]      Price Feed（Index/Mark，離群值剔除）
[KAFKA]   事件匯流排（order.*, trade.*, position.*, ...）
[ETL]     Stream ETL/Replay（入倉、校驗、重放）
[CH]      ClickHouse（OLAP，報表/查詢）
[ADM]     Admin/BI（報表 API/看板/告警）

──────────────────────────────────────────────────────────────────────────────
Scenario A：LIMIT/IOC → 立即全成（Taker），交易一路入報表
──────────────────────────────────────────────────────────────────────────────
[Client] -> [GW]     : POST /fapi/v1/order (symbol, side, type=LIMIT/IOC, qty, price, clientOrderId)
[GW]     -> [ORD]    : route + auth context
[ORD]    -> [RISK]   : PreCheck(uid, symbol, intent, notional, mode, leverage)
[RISK]   -> [ORD]    : OK(requiredIM, riskTier)   # 通過風控
[ORD]    -> [WAL]    : reserve(uid, asset=USDT, amount=requiredIM, ref=orderId)
[WAL]    -> [ORD]    : reserved(holdId, balanceAfter)
[ORD]    -> [ME]     : place(order)               # 進撮合序列
[ME]     -> [ME]     : price-time priority match  # 與對手方撮合
[ME]     -> [ORD]    : fillReport(orderId, trades[...], allFilled=true, fees, maker=false)
[ME]     -> [MD]     : book deltas / aggTrade     # 公有WS（行情）
[ME]     -> [KAFKA]  : emit trade.executed(matchId, taker/maker, fee, ts, schemaVersion)

# 結算/持倉
[ORD]    -> [POS]    : applyTrade(trades)         # 更新均價、UPnL/RPnL
[POS]    -> [WAL]    : settle(ledger postings: fee, realized PnL, release unused IM)
[WAL]    -> [POS]    : booked(ledgerIds...)
[POS]    -> [KAFKA]  : emit position.changed(uid, symbol, qty, entry, upnl, ts)
[ORD]    -> [Client] : REST response {orderId, status=FILLED, fills=[...]} (私有WS同步推送)
[MD]     -> [Client] : ws: @aggTrade, @depth (public)
[ORD]    -> [KAFKA]  : emit order.updated(FILLED)

# 報表鏈路
[KAFKA]  -> [ETL]    : trade.executed, position.changed, order.updated (stream)
[ETL]    -> [CH]     : insert into trades_all / positions_snapshots (w/ materialized views)
[ADM]    -> [CH]     : query: volume, fees, maker/taker ratio, user statement
[CH]     -> [ADM]    : result (BI dashboards/alerts)

──────────────────────────────────────────────────────────────────────────────
Scenario B：MARKET/部分成交 → 剩餘轉 Maker 掛簿（可選），之後再成交
──────────────────────────────────────────────────────────────────────────────
[Client] -> [GW]     : POST /fapi/v1/order (type=MARKET, qty=100)
[GW]     -> [ORD]    : route
[ORD]    -> [RISK]   : PreCheck(...)
[RISK]   -> [ORD]    : OK(requiredIM_for_worst_case)
[ORD]    -> [WAL]    : reserve(requiredIM_for_worst_case)
[ORD]    -> [ME]     : place(order=MKT)
[ME]     -> [ORD]    : partial fills: 60 filled, 40 remaining
[ME]     -> [KAFKA]  : emit trade.executed (qty=60)
[ORD]    -> [POS]    : applyTrade(60)
[POS]    -> [WAL]    : settle fee/RPnL (partial), adjust holds
[ORD]    -> [ME]     : policy: convert remaining 40 to LIMIT @ lastPrice (maker=true)  # 可配置
[ME]     -> [MD]     : book deltas (bid/ask updated)
[ORD]    -> [Client] : response {status=PARTIALLY_FILLED, executedQty=60, rest=40 on book}
...稍後市場對手出現...
[ME]     -> [ORD]    : fills for remaining 40 → allFilled
[ME]     -> [KAFKA]  : emit trade.executed (qty=40, maker=true)
[ORD]    -> [POS]    : applyTrade(40) → position/avg updated
[POS]    -> [WAL]    : settle fee/RPnL (final), release unused IM
[ORD]    -> [KAFKA]  : order.updated(FILLED)
[ETL]    -> [CH]     : all trades & position snapshots ready for reports

──────────────────────────────────────────────────────────────────────────────
Scenario C：撤單（含預扣釋放）
──────────────────────────────────────────────────────────────────────────────
[Client] -> [GW]     : DELETE /fapi/v1/order?symbol&orderId
[GW]     -> [ORD]    : route
[ORD]    -> [ME]     : cancel(orderId)
[ME]     -> [ORD]    : canceled(ok, remainingQty)
[ORD]    -> [WAL]    : release(holdId, amount=unusedIM)
[WAL]    -> [ORD]    : released(balanceAfter)
[ORD]    -> [KAFKA]  : order.canceled
[ORD]    -> [Client] : response {status=CANCELED}

──────────────────────────────────────────────────────────────────────────────
依賴：標記價/資金費觸發的風控與報表
──────────────────────────────────────────────────────────────────────────────
[PX]     -> [KAFKA]  : mark_price.updated(symbol, mark, ts)
[KAFKA]  -> [RISK]   : consume mark → recompute health → maybe trigger liquidation
[RISK]   -> [ME]     : place(liq order, IOC, protected price)
[ME]     -> [KAFKA]  : liquidation.triggered / .filled / adl.executed
[ETL]    -> [CH]     : write funding_settled / liquidation streams
[ADM]    -> [CH]     : risk board: liq success rate, ADL count, insurance fund delta

# 通知與可觀測性（橫切面）
[ORD]/[POS]/[WAL] -> [Client] : Private WS: order/account/position updates
[MD]              -> [Client] : Public WS: depth, bookTicker, aggTrade, markPrice
[ETL]/[CH]        -> [ADM]    : metrics & alerts (reporting delay, stream lag, p99 matching latency)
```