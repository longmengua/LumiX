# 18 Testing & Go Live：測試、壓測、上線檢查

## 任務

建立交易所 OL 的測試清單、驗收標準、壓測項目與上線檢查表。  
本任務是 OL 前 launch blocker 清單。可先建立 docs、test plan、stub tests，但文件必須明確標示哪些項目是上線必要項，未通過即阻擋上線。
後端測試預期對應 Java 21 + Spring Boot 3；前端仍維持 web/ React + TypeScript + Vite。

---

## 測試類型

| 類型 | 說明 |
|---|---|
| 單元測試 | service、風控、計算 |
| 整合測試 | 下單、結算、充值、提現 |
| 帳本測試 | 入帳、扣帳、凍結、解凍、冪等 |
| 合約測試 | 倉位、保證金、PnL、資金費率 |
| 強平測試 | 強平價格、強平流程、保險基金 |
| 槓桿測試 | 借幣、還款、利息、風險率 |
| API 測試 | 簽名、timestamp、IP、限流 |
| 後台測試 | 權限、操作日誌、審批 |
| 對帳測試 | 快照、差異、補償 |
| 壓測 | 下單、撤單、行情、WebSocket |

---

## OL 必測場景

### 資產帳本

```text
入帳成功。
扣帳成功。
凍結成功。
解凍成功。
扣凍結成功。
重複 idempotency key 不重複處理。
可用 + 凍結 + 負債邏輯正確。
不允許非法負數。
```

### 現貨

```text
買單凍結 USDT。
賣單凍結 BTC / ETH。
撤單解凍。
部分成交。
完全成交。
手續費正確。
成交結算冪等。
```

### 合約

```text
開多。
開空。
加倉。
減倉。
平倉。
反向開倉。
未實現盈虧。
已實現盈虧。
保證金。
強平價格。
資金費率。
```

### 強平

```text
標記價格變動觸發強平。
取消未成交委託。
部分強平。
全部強平。
保險基金入帳。
保險基金出帳。
穿倉紀錄。
```

### 槓桿

```text
借幣。
還款。
利息累計。
風險率預警。
槓桿強平。
壞帳紀錄。
```

---

## 上線最低標準

| 類別 | 標準 |
|---|---|
| 充值 | 可入帳、可對帳 |
| 提現 | 可審核、可暫停 |
| 現貨 | 可下單、撤單、成交、結算 |
| 合約 | 可開倉、平倉、算盈虧、強平 |
| 槓桿 | 可借幣、還款、計息、強平 |
| API | 有簽名、限流、IP 白名單 |
| 做市商 | 可接入、可停用 |
| 後台 | 可查詢、可審核、可 kill switch |
| 對帳 | 每日可產出結果 |
| 日誌 | 高危操作有紀錄 |

## OL 必要測試

```text
C++ Core integration test
Java Settlement integration test
Ledger reconciliation test
Order lifecycle test
Cancel order test
Partial fill test
Full fill test
WebSocket orderbook snapshot + diff test
sequence gap recovery test
crash recovery test
replay test
load test
```

---

## 壓測項目

| 項目 | 說明 |
|---|---|
| 下單 TPS | 現貨與合約 |
| 撤單 TPS | 做市商高頻撤單 |
| 撮合延遲 | 單交易對 |
| 行情推送 | WebSocket |
| API Gateway | 簽名與限流 |
| 帳本寫入 | 結算與流水 |
| 強平掃描 | mark price 更新 |
| 對帳任務 | 大量資料 |

---

## 事故演練

```text
暫停提現。
暫停單一現貨交易對。
暫停單一合約交易對。
合約僅減倉。
停用做市商。
停用內部 MM。
凍結用戶。
API key 洩漏處理。
充值 callback 重複。
成交未結算。
對帳不平。
```

---

## 驗收標準

```text
有完整測試計畫文件。
有上線前 checklist。
有事故演練清單。
有壓測項目清單。
核心帳本與交易核心測試必須列入 OL 前完成項。
合約與槓桿風險測試有明確場景。
高風險測試仍需 `TODO: requires high-reasoning review before production use`，但不能把 OL 必要項延後。
TODO: requires high-reasoning review before production use
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
