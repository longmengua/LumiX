# 12 Open API：API Key、簽名、限流、做市商接入

## 任務

建立 Open API 模組，支援現貨、合約、槓桿的 API 入口與安全機制。  
本任務先做 API key、簽名、IP 白名單、rate limit 與 route 骨架。

---

## API Key 權限

| 權限 | 說明 |
|---|---|
| read | 查詢 |
| spot_trade | 現貨交易 |
| futures_trade | 合約交易 |
| margin_trade | 槓桿交易 |
| withdraw | 預設不開 |
| market_maker | 做市商權限 |
| internal_mm | 內部做市權限 |

---

## 安全要求

```text
API secret 只在建立時顯示一次。
secret 不可明文保存。
所有 private API 需要 timestamp。
所有 private API 需要 signature。
timestamp 需要過期檢查。
做市商 API key 必須支援 IP 白名單。
API key 可以停用與刪除。
所有 API key 操作寫 security log。
withdraw 權限預設不可開。
```

---

## Rate Limit

| 等級 | 用途 |
|---|---|
| retail | 一般用戶 |
| vip | 高階用戶 |
| market_maker | 外部做市商 |
| internal_mm | 內部做市商 |
| admin | 後台內部 |

---

## Public API

| API | 說明 |
|---|---|
| time | 伺服器時間 |
| symbols | 交易對 |
| depth | 深度 |
| trades | 最新成交 |
| ticker | ticker |
| kline | K 線 |
| mark price | 標記價格 |
| funding rate | 資金費率 |

---

## Private API

| 類型 | API |
|---|---|
| 帳戶 | 查資產、查帳戶摘要 |
| 現貨 | 下單、撤單、查單、查成交 |
| 合約 | 下單、撤單、查倉位、調槓桿、查資金費率 |
| 槓桿 | 借幣、還款、查負債、查風險率 |
| API Key | 查 key、建立、停用、刪除 |

---

## 簽名流程

```text
Client 組參數
  ↓
加入 timestamp
  ↓
使用 secret 產生 signature
  ↓
送出 request
  ↓
Gateway 查 API key
  ↓
檢查 IP 白名單
  ↓
檢查 timestamp
  ↓
驗證 signature
  ↓
檢查權限
  ↓
檢查 rate limit
  ↓
轉發到內部 service
```

---

## 不做範圍

```text
不要直接實作撮合。
不要直接實作強平。
不要繞過內部交易 service。
Open API 只做安全閘道與 service adapter。
```

---

## 驗收標準

```text
可以建立 API key。
secret 只顯示一次。
可以停用與刪除 API key。
private API 需要 timestamp 與 signature。
IP 白名單可以配置。
rate limit 有等級設計。
現貨、合約、槓桿 API route 有骨架。
權限不足會拒絕。
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
