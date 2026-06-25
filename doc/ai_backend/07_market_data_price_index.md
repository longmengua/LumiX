# 07 Market Data / Price Index：行情、K 線、指數價、標記價

## 任務

建立行情服務、K 線服務、外部價格源、指數價格與標記價格骨架。  
此模組是現貨、合約、強平、做市商都會依賴的基礎。

---

## 行情範圍

| 類型 | 說明 |
|---|---|
| Order Book | 現貨與合約深度 |
| 最新成交 | trade stream |
| Ticker | 最新價與 24h 統計 |
| K 線 | 1m、5m、15m、1h、1d |
| WebSocket | 公共行情與私有推送 |
| 指數價格 | 外部價格源聚合 |
| 標記價格 | 合約盈虧與強平使用 |

---

## 外部價格源

第一版可以先 stub，或使用 adapter 介面。

| 來源 | 用途 |
|---|---|
| Binance | 參考價格 |
| OKX | 參考價格 |
| Bybit | 參考價格 |
| Coinbase | 可選 |

---

## 指數價格規則

```text
收集多個外部價格。
剔除過期價格。
剔除偏離過大的價格。
取中位數或加權平均。
產生 index price。
寫入 price_index 紀錄。
```

---

## 標記價格規則

```text
使用 index price。
結合合理基差或 funding basis。
產生 mark price。
mark price 用於：
1. 未實現盈虧
2. 保證金率
3. 強平判斷
4. 風控
```

---

## K 線流程

```text
成交事件
  ↓
按 symbol + interval 聚合
  ↓
更新 open / high / low / close / volume
  ↓
寫入 kline
  ↓
WebSocket 推送
```

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| market_trade | 最新成交，可共用現貨 / 合約成交 |
| kline | K 線 |
| ticker_24h | 24h ticker |
| orderbook_snapshot | 深度快照，可先 cache |
| external_price_source | 外部價格 |
| price_index | 指數價格 |
| mark_price | 標記價格 |

---

## API 需求

| API | 說明 |
|---|---|
| 查交易對 | 現貨與合約 |
| 查深度 | depth |
| 查最新成交 | trades |
| 查 ticker | ticker |
| 查 K 線 | kline |
| 查指數價格 | index price |
| 查標記價格 | mark price |
| WebSocket depth | 深度推送 |
| WebSocket trade | 成交推送 |
| WebSocket ticker | ticker 推送 |
| WebSocket kline | K 線推送 |

---

## 不做範圍

```text
不要實作撮合。
不要實作合約強平。
不要實作真實高頻行情架構。
外部價格源可以先用 adapter / stub。
```

---

## 驗收標準

```text
可以查現貨與合約交易對。
可以查 depth、trades、ticker、kline。
可以產生或 mock index price。
可以產生或 mock mark price。
K 線資料結構支援多週期。
WebSocket channel 有路由或服務骨架。
合約模組可依賴 mark price 介面。
```

---

## Codex 回覆格式

請依照以下格式回覆：

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

風險與注意事項：
- ...
```
