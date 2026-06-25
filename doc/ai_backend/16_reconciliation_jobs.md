# 16 Reconciliation Jobs：對帳與補償任務

## 任務

建立每日對帳與補償任務骨架。  
交易所上線前必須能對資產、錢包、訂單、成交、倉位、借貸、費用與保險基金。
後端實作預期為 Java 21 + Spring Boot 3；對帳與補償 job 優先以 PostgreSQL 快照與排程 skeleton 表達。
對帳來源包含 Java 業務服務與 C++ Core 輸出的事件流，事件必須可重放。

---

## 對帳範圍

| 對帳項 | 說明 |
|---|---|
| 現貨資產 vs 現貨流水 | 現貨帳戶 |
| 合約資產 vs 合約流水 | 合約帳戶 |
| 槓桿資產 vs 槓桿流水 | 槓桿帳戶 |
| 鏈上錢包 vs 內部餘額 | 準備金 |
| 訂單 vs 成交 | 撮合結果 |
| 成交 vs 結算 | 是否完成資產結算 |
| 倉位 vs 成交 | 合約倉位 |
| C++ Core event stream | event_id、sequence、symbol、timestamp |
| 未實現盈虧 vs 標記價格 | 風險準確 |
| 資金費率 vs 流水 | funding |
| 借款 vs 負債 | margin debt |
| 利息 vs 流水 | margin interest |
| 手續費 vs 平台收入 | revenue |
| 保險基金 vs 強平 | liquidation |

---

## 每日對帳流程

```text
生成現貨帳戶快照
  ↓
生成合約帳戶快照
  ↓
生成槓桿帳戶快照
  ↓
彙總資產流水
  ↓
比對充值與提現
  ↓
比對現貨訂單與成交
  ↓
比對合約訂單、成交、倉位
  ↓
比對槓桿借款、利息、還款
  ↓
比對手續費與平台收入
  ↓
比對資金費率
  ↓
比對保險基金
  ↓
產出對帳結果
  ↓
異常建立工單或告警
```

---

## 補償任務

| 任務 | 說明 |
|---|---|
| 充值 callback 補償 | callback 失敗 |
| 提現廣播補償 | broadcast 失敗 |
| 提現狀態同步 | 查鏈上狀態 |
| 現貨結算補償 | 成交未結算 |
| 合約倉位補償 | 倉位更新失敗 |
| 資金費率補償 | funding 失敗 |
| 利息補償 | interest job 失敗 |
| 強平補償 | liquidation 流程異常 |
| K 線補償 | 缺失 K 線 |
| 對帳異常工單 | 人工處理 |

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| daily_asset_snapshot | 資產快照 |
| daily_position_snapshot | 倉位快照 |
| daily_margin_snapshot | 槓桿快照 |
| daily_reconciliation_result | 對帳結果 |
| reconciliation_mismatch | 對帳差異 |
| compensation_task | 補償任務 |
| compensation_log | 補償執行紀錄 |

---

## API / 後台需求

| API | 說明 |
|---|---|
| 查每日對帳 | list |
| 查對帳差異 | mismatch |
| 重跑對帳 | rerun |
| 查補償任務 | tasks |
| 重試補償 | retry |
| 標記人工處理 | resolve |

---

## 不做範圍

```text
不要直接修資產。
不要自動調帳。
異常只能產生工單或補償任務。
人工調帳必須走帳本與審批。
對 C++ Core 事件的缺失或順序錯誤，先進入補償任務與人工對帳，不可直接修改撮合結果。
```

---

## 驗收標準

```text
可以建立每日快照資料結構。
可以產生對帳結果。
可以記錄對帳差異。
可以建立補償任務。
可以重試補償任務。
後台可以查對帳結果與差異。
不會直接自動修改資產。
對帳修復策略只保留 interface / skeleton / TODO。
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
