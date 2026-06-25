# 08 Spot Trading：現貨交易

## 任務

建立現貨交易模組骨架，包含訂單、撤單、成交、結算、手續費與行情事件。  
後端實作預期為 Java 21 + Spring Boot 3。
正式撮合核心目標為 C++ Core；MVP 階段可先保留 Java interface / skeleton / stub。

---

## 功能範圍

| 功能 | MVP |
|---|---|
| 限價單 | 必要 |
| 市價單 | 建議，需價格保護 |
| 撤單 | 必要 |
| 查單 | 必要 |
| 當前委託 | 必要 |
| 歷史委託 | 必要 |
| 成交紀錄 | 必要 |
| 手續費 | 必要 |
| 成交結算 | 必要 |
| Order Book 接入 | 必要 |

---

## 訂單狀態

```text
NEW
PARTIALLY_FILLED
FILLED
CANCELED
REJECTED
EXPIRED
```

---

## 下單流程

```text
用戶 / 做市商下單
  ↓
驗證 symbol、price、quantity、precision
  ↓
交易風控
  ↓
呼叫帳本凍結資產
  ↓
建立現貨訂單
  ↓
Java Order Service 透過 `MatchingEngineClient` / gRPC / event bus 將訂單送入 C++ Core
  ↓
成交或掛單
  ↓
成交事件進入 settlement / ledger service
  ↓
扣買賣雙方資產與手續費
  ↓
推送行情與訂單更新
```

---

## 撤單流程

```text
用戶 / 做市商撤單
  ↓
查詢訂單狀態
  ↓
若可撤，通知 Java Order Service，再由 `MatchingEngineClient` / gRPC / event bus 轉給 C++ Core
  ↓
更新訂單狀態
  ↓
解凍剩餘資產
  ↓
推送訂單更新
```

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| spot_order | 現貨訂單 |
| spot_trade | 現貨成交 |
| spot_settlement | 現貨結算 |
| order_event | 訂單事件 |
| fee_config | 費率配置 |
| matching_engine_event | 由 C++ Core 輸出的 order / trade / orderbook event |

---

## API 需求

| API | 說明 |
|---|---|
| 下單 | limit / market |
| 撤單 | cancel |
| 查單 | order detail |
| 查當前委託 | open orders |
| 查歷史委託 | order history |
| 查成交 | trade history |

---

## 資產要求

```text
買單凍結 quote asset，例如 USDT。
賣單凍結 base asset，例如 BTC。
撤單解凍剩餘資產。
成交結算必須冪等。
手續費必須寫資產流水。
不得直接修改餘額。
Matching Engine / C++ Core 不得直接修改資產或 ledger，只能產生事件。
Settlement / Ledger Service 負責資產結算與資產流水。
所有 C++ Core 輸出事件必須包含 `event_id`、`sequence`、`symbol`、`timestamp`，支援重放、對帳、補償。
```

---

## 不做範圍

```text
不要實作合約。
不要實作槓桿借貸。
不要實作內部做市策略。
如果 matching engine 未完成，先用 Java interface / stub / TODO。
現貨成交與結算核心正式目標為 C++ Core，但 MVP 階段僅保留 Java interface / skeleton / TODO。
TODO: requires high-reasoning review before production use
```

---

## 驗收標準

```text
可以建立現貨訂單。
下單會凍結資產。
撤單會解凍資產。
可以查當前委託與歷史委託。
成交結算具備 service 入口與冪等設計。
手續費有計算與流水設計。
API 有基本錯誤處理。
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
