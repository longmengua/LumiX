# 13 Market Maker：外部做市商與內部做市 Bot

## 任務

建立做市商管理模組，包含外部做市商接入、API 權限、績效報表，以及內部做市 bot 的安全骨架。

---

## 外部做市商接入流程

```text
KYB / 合約簽署
  ↓
建立做市商帳號
  ↓
設定現貨 / 合約 / 槓桿權限
  ↓
建立 API key
  ↓
綁定 IP 白名單
  ↓
Sandbox 測試
  ↓
小流量試運行
  ↓
正式上線
  ↓
每日績效報表
```

---

## 做市商能力

| 能力 | 說明 |
|---|---|
| 現貨下單 | 做現貨深度 |
| 合約下單 | 做合約深度 |
| 批量撤單 | 撤過期報價 |
| 查 open orders | 管理訂單 |
| 查成交 | 管理庫存與 PnL |
| 查倉位 | 合約做市必需 |
| 查保證金 | 避免爆倉 |
| WebSocket | 行情與訂單推送 |
| 高 rate limit | 做市專用 |
| IP 白名單 | 安全要求 |

---

## 做市商 SLA

| 指標 | MVP |
|---|---|
| 現貨雙邊報價 | 必須 |
| 合約雙邊報價 | 必須 |
| spread | 需監控 |
| depth | 需監控 |
| uptime | 需監控 |
| order alive time | 需監控 |
| cancel ratio | 需監控 |
| API latency | 需監控 |
| 倉位風險 | 需監控 |

---

## 內部做市商原則

```text
內部做市商必須走 Open API。
內部做市商不得直接改 order book。
內部做市商不得繞過帳本。
內部做市商不得繞過撮合。
內部做市商必須有最大庫存、最大倉位、最大虧損限制。
內部做市商必須有 kill switch。
```

---

## 內部做市流程

```text
讀外部價格源
  ↓
計算參考價格
  ↓
讀自家 order book
  ↓
讀自身庫存與倉位
  ↓
計算 bid / ask
  ↓
檢查風控限制
  ↓
透過 Open API 掛單
  ↓
監控成交
  ↓
調整報價與庫存
  ↓
異常時撤單或停止
```

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| market_maker_account | 做市商帳號 |
| market_maker_api_key | 可復用 api_key |
| market_maker_performance | 做市績效 |
| market_maker_sla_config | SLA 配置 |
| internal_mm_config | 內部 MM 設定 |
| internal_mm_risk_log | 內部 MM 風控紀錄 |

---

## 後台需求

| 功能 | 說明 |
|---|---|
| 建立做市商 | 外部 / 內部 |
| 設定權限 | 現貨、合約、槓桿 |
| 設定 rate limit | 做市商等級 |
| 查績效 | spread、depth、volume |
| 停用做市商 | kill switch |
| 停止內部 MM | emergency |
| 查 MM 風險 | 庫存、倉位、PnL |

---

## 不做範圍

```text
不要實作複雜做市策略。
不要直接接入真實外部交易所下單。
不要繞過 Open API。
可先實作配置、帳號、報表與 bot stub。
```

---

## 驗收標準

```text
可以建立做市商帳號。
可以配置做市商權限與 rate limit。
可以綁定 API key 與 IP 白名單。
可以查做市商績效報表骨架。
可以停用做市商。
內部 MM 有設定與 kill switch。
內部 MM 下單路徑必須走 Open API adapter。
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
