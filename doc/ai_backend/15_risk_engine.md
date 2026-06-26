# 15 Risk Engine：風控與 Kill Switch

## 任務

建立交易所風控模組骨架，包含帳號、提現、現貨、合約、槓桿、API、做市商與系統級 Kill Switch。
後端實作預期為 Java 21 + Spring Boot 3；風控規則與開關可先以 Redis / PostgreSQL skeleton 表達，但這是 OL launch blocker，不是可延後項。

---

## 帳號風控

| 風控項 | 說明 |
|---|---|
| 異常登入 | 新 IP、新設備 |
| 密碼重置鎖定 | 修改後限制提現 |
| 2FA 重置審核 | 高風險 |
| 用戶凍結 | 禁止登入或交易 |
| 安全操作紀錄 | 審計 |

---

## 提現風控

| 風控項 | 說明 |
|---|---|
| 新地址延遲 | 新增地址後延遲提現 |
| 大額提現審核 | 超額人工審核 |
| 每日限額 | 依 KYC 等級 |
| 黑名單地址 | 禁止提現 |
| 提現暫停 | 系統級開關 |

---

## 現貨風控

| 風控項 | 說明 |
|---|---|
| 價格偏離保護 | 限制離譜價格 |
| 市價單滑點 | 防止打穿 |
| 最小下單量 | 防垃圾單 |
| 最大下單量 | 防大單 |
| 撤單率監控 | 防刷單 |
| 自成交監控 | 防洗量 |

---

## 合約風控

| 風控項 | 說明 |
|---|---|
| 最大槓桿 | 不同合約不同上限 |
| 風險限額 | 階梯保證金 |
| 標記價格 | 強平依據 |
| 僅減倉 | 極端模式 |
| 強平保護 | 防打穿市場 |
| 大戶持倉 | 監控集中風險 |

---

## 槓桿風控

| 風控項 | 說明 |
|---|---|
| 最大借款額度 | 控制負債 |
| 可借資產池 | 控制借貸資產 |
| 風險率 | 預警與強平 |
| 借幣暫停 | emergency |
| 抵押折扣 | 高風險資產折扣 |
| 壞帳處理 | 強平不足 |

---

## Kill Switch

| 開關 | 說明 |
|---|---|
| 暫停全部交易 | 全站交易事故 |
| 暫停現貨交易對 | 單一 spot |
| 暫停合約交易對 | 單一 futures |
| 合約僅減倉 | 禁止開倉 |
| 暫停槓桿借幣 | borrow pause |
| 暫停提現 | withdraw pause |
| 停用 API key | api risk |
| 停用做市商 | MM risk |
| 停止內部 MM | internal MM risk |
| 凍結用戶 | user risk |
| 凍結資產 | asset risk |

---

## API / Service

| 介面 | 說明 |
|---|---|
| check_account_risk | 帳號風控 |
| check_withdraw_risk | 提現風控 |
| check_spot_order_risk | 現貨下單風控 |
| check_futures_order_risk | 合約下單風控 |
| check_margin_risk | 槓桿風控 |
| check_api_risk | API 風控 |
| apply_kill_switch | 執行開關 |
| query_risk_hits | 查風控命中 |

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| risk_rule | 風控規則 |
| risk_hit_log | 命中紀錄 |
| kill_switch_config | 系統開關 |
| blacklist | 黑名單 |
| whitelist | 白名單 |
| user_risk_flag | 用戶風險標籤 |

---

## 驗收標準

```text
有風控規則資料結構。
可以記錄風控命中。
可以檢查提現風控。
可以檢查現貨、合約、槓桿下單風控。
可以設定與查詢 kill switch。
交易、提現、API 可依 kill switch 阻擋。
後台可查風控命中紀錄。
風控公式與分數只保留 skeleton / TODO，但 OL 前必須完成 review 與驗證。
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
