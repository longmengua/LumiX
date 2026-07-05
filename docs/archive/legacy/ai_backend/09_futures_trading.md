# 09 Futures Trading：U 本位永續合約

## 任務

建立 U 本位永續合約模組骨架，包含合約訂單、倉位、保證金、盈虧、資金費率與標記價格接入。
後端實作預期為 Java 21 + Spring Boot 3；交易核心只保留骨架、interface、TODO。
正式撮合核心為 C++ Core，這是 OL 前必要項；合約訂單與事件流必須接通正式 C++ Core，不能以 Java interface 取代正式流程。

---

## 第一版合約

| 合約 | 說明 |
|---|---|
| BTCUSDT Perp | U 本位永續 |
| ETHUSDT Perp | U 本位永續 |
| SOLUSDT Perp | U 本位永續 |

---

## 功能範圍

| 功能 | OL 必要 |
|---|---|
| 限價單 | 必要 |
| 市價單 | 建議，需價格保護 |
| 開倉 | 必要 |
| 平倉 | 必要 |
| 撤單 | 必要 |
| 查倉位 | 必要 |
| 調整槓桿 | 必要 |
| 保證金計算 | 必要 |
| 未實現盈虧 | 必要 |
| 已實現盈虧 | 必要 |
| 資金費率 | 必要 |
| 標記價格 | 必要 |
| 指數價格 | 必要 |

---

## 倉位概念

| 欄位 | 說明 |
|---|---|
| symbol | 合約 |
| side | long / short |
| size | 倉位數量 |
| entry_price | 開倉均價 |
| mark_price | 標記價格 |
| leverage | 槓桿 |
| margin | 保證金 |
| unrealized_pnl | 未實現盈虧 |
| realized_pnl | 已實現盈虧 |
| liquidation_price | 強平價格 |
| margin_mode | isolated / cross，可先 isolated |

---

## 下單流程

```text
提交合約訂單
  ↓
檢查合約、價格、數量、精度
  ↓
檢查槓桿與風險限額
  ↓
計算初始保證金
  ↓
檢查合約帳戶可用保證金
  ↓
凍結保證金
  ↓
建立合約訂單
  ↓
Java Order Service 透過 `MatchingEngineClient` / gRPC / event bus 將訂單送入 C++ Core
  ↓
成交後更新倉位
  ↓
更新保證金與盈虧
  ↓
推送訂單與倉位更新
```

---

## 倉位更新流程

```text
成交事件
  ↓
查詢當前倉位
  ↓
判斷開倉、加倉、減倉、平倉、反向
  ↓
更新倉位數量與均價
  ↓
計算已實現盈虧
  ↓
計算未實現盈虧
  ↓
更新保證金占用
  ↓
更新強平價格
  ↓
寫倉位流水
```

---

## 資金費率流程

```text
到達 funding interval
  ↓
讀取所有持倉
  ↓
根據多空方向計算應收 / 應付
  ↓
呼叫帳本扣款 / 入帳
  ↓
寫資金費率紀錄
  ↓
納入對帳
```

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| futures_order | 合約訂單 |
| futures_trade | 合約成交 |
| futures_position | 當前倉位 |
| futures_position_history | 倉位歷史 |
| futures_margin | 保證金 |
| futures_pnl_journal | 盈虧流水 |
| funding_rate | 資金費率 |
| funding_fee_record | 資金費扣收 |
| risk_limit_config | 風險限額 |

---

## API 需求

| API | 說明 |
|---|---|
| 下合約單 | 開倉 / 平倉 |
| 撤合約單 | cancel |
| 查合約單 | detail |
| 查當前委託 | open orders |
| 查成交 | trades |
| 查倉位 | positions |
| 調整槓桿 | leverage |
| 查保證金 | margin |
| 追加保證金 | isolated margin |
| 查資金費率 | funding rate |
| 查資金費紀錄 | funding history |

---

## 不做範圍

```text
不要實作完整強平，交給 10_liquidation_insurance_fund.md。
不要實作幣本位合約。
不要實作期權。
不要實作組合保證金。
不要實作 production 級 PnL / 保證金 / 資金費率公式。
不要在此文件實作 production 級撮合。
OL 前不得使用 mock matching / mock order book / mock trade / mock settlement 作為正式流程。
TODO: requires high-reasoning review before production use
```

---

## 驗收標準

```text
可以建立合約訂單。
可以建立與更新倉位。
可以查當前倉位。
可以計算未實現盈虧與已實現盈虧。
可以接入 mark price。
可以設定與查詢槓桿。
資金費率有資料結構與扣收流程骨架。
所有資產變動透過帳本服務。
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
