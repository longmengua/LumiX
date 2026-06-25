# 06 Wallet：充值、提現、錢包 Gateway

## 任務

建立充值、提現與錢包 Gateway 骨架。  
第一版可以先用 stub / mock chain scanner，但資料結構、狀態流、冪等與後台審核必須先設計好。

---

## 支援資產優先級

| 優先級 | 資產 |
|---|---|
| 第一優先 | USDT-TRC20 |
| 第二優先 | BTC、ETH-ERC20 |
| 第三優先 | SOL |

---

## 充值功能

| 功能 | 說明 |
|---|---|
| 充值地址 | 分配或查詢用戶地址 |
| 充值紀錄 | tx hash、地址、金額、confirmation、狀態 |
| 掃鏈任務 | 可先 stub |
| confirmation | 可配置確認數 |
| 入帳 | 呼叫帳本服務 |
| callback log | 記錄 callback 與重試 |
| 冪等 | tx hash + asset + chain 唯一 |

---

## 提現功能

| 功能 | 說明 |
|---|---|
| 提現地址 | 新增、查詢、白名單 |
| 提現申請 | 用戶提交 |
| 安全驗證 | 預留 Email、SMS、GA |
| 風控 | 大額、新地址、黑名單 |
| 資產凍結 | 呼叫帳本服務凍結 |
| 後台審核 | 審核通過或拒絕 |
| 鏈上廣播 | 可先 stub |
| 狀態同步 | 查鏈上狀態，可先 stub |
| 失敗解凍 | 提現失敗要解凍 |
| 成功扣凍結 | 提現成功扣除 frozen |

---

## 狀態設計

### 充值狀態

```text
PENDING
CONFIRMING
SUCCESS
FAILED
IGNORED
```

### 提現狀態

```text
SUBMITTED
RISK_REVIEW
ADMIN_REVIEW
APPROVED
REJECTED
BROADCASTING
CHAIN_PENDING
SUCCESS
FAILED
CANCELED
```

---

## 流程：充值

```text
取得充值地址
  ↓
鏈上轉帳
  ↓
掃鏈發現交易
  ↓
等待 confirmation
  ↓
檢查 tx hash 是否已處理
  ↓
呼叫帳本入帳
  ↓
寫充值紀錄與 callback log
  ↓
狀態改 SUCCESS
```

---

## 流程：提現

```text
提交提現申請
  ↓
安全驗證
  ↓
風控檢查
  ↓
凍結資產
  ↓
後台審核
  ↓
廣播鏈上交易
  ↓
等待鏈上確認
  ↓
扣除凍結資產
  ↓
狀態改 SUCCESS
```

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| deposit_address | 充值地址 |
| deposit_record | 充值紀錄 |
| withdraw_address | 提現地址 |
| withdraw_record | 提現紀錄 |
| wallet_callback_log | callback 與重試 |
| wallet_hot_balance | 熱錢包餘額 |
| wallet_chain_config | 鏈與確認數配置 |

---

## API 需求

| API | 說明 |
|---|---|
| 查充值地址 | user + asset + chain |
| 查充值紀錄 | 分頁 |
| 新增提現地址 | 預留安全驗證 |
| 查提現地址 | 白名單 |
| 申請提現 | 建立 withdraw |
| 查提現紀錄 | 分頁 |
| 後台查充值 | admin |
| 後台查提現 | admin |
| 後台審核提現 | approve / reject |
| 後台重試 callback | retry |

---

## 不做範圍

```text
不要真實接私鑰簽名。
不要實作完整鏈上節點。
不要把私鑰放進程式碼。
不要繞過帳本直接改餘額。
```

---

## 驗收標準

```text
可以取得充值地址。
可以查充值紀錄。
可以提交提現申請。
提現申請會凍結資產。
提現拒絕會解凍資產。
提現成功會扣除凍結資產。
充值入帳具備 tx hash 冪等。
後台可以查詢與審核提現。
錢包掃鏈與廣播可先 stub，但流程狀態完整。
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
