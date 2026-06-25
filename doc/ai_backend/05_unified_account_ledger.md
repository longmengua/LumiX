# 05 Unified Account Ledger：統一帳戶與資產帳本

## 任務

建立交易所核心資產帳本骨架。  
所有現貨、合約、槓桿、錢包、做市商、平台收入都要依賴這套帳本。
後端實作預期為 Java 21 + Spring Boot 3，資料層優先 PostgreSQL，快取可用 Redis。
正式交易撮合核心由 C++ Core 負責，C++ Core 只能輸出事件，不可直接修改帳本資料。

---

## 核心帳戶

| 帳戶 | 說明 |
|---|---|
| 現貨帳戶 | 充值、提現、現貨交易 |
| 合約帳戶 | 永續合約保證金、盈虧、資金費率 |
| 槓桿帳戶 | 借幣、還款、負債、利息 |
| 平台收入帳戶 | 手續費、提幣費、利息收入 |
| 保險基金帳戶 | 強平與穿倉處理 |
| 做市商帳戶 | 外部 / 內部做市資產 |

---

## 帳本能力

| 能力 | 說明 |
|---|---|
| 入帳 | 充值、盈虧、轉入 |
| 扣帳 | 提現、手續費、虧損 |
| 凍結 | 下單、提現、保證金占用 |
| 解凍 | 撤單、提現失敗、保證金釋放 |
| 劃轉 | 現貨、合約、槓桿之間互轉 |
| 借入 | 槓桿借幣 |
| 還款 | 歸還本金與利息 |
| 資金費率 | 合約多空支付 |
| 強平扣帳 | 強平時扣保證金 |
| 保險基金 | 承接穿倉與強平盈餘 |

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| account | 用戶不同帳戶 |
| user_balance | 帳戶資產餘額 |
| ledger_journal | 資產流水 |
| account_transfer | 帳戶劃轉 |
| fee_journal | 費用流水，可合併 ledger |
| idempotency_record | 冪等紀錄，可合併實作 |
| daily_asset_snapshot | 每日快照 |

---

## 必要欄位概念

| 欄位 | 說明 |
|---|---|
| user_id | 用戶 |
| account_type | spot、futures、margin、income、insurance |
| asset | USDT、BTC、ETH 等 |
| available | 可用 |
| frozen | 凍結 |
| debt | 負債，主要用於槓桿 |
| interest | 利息，主要用於槓桿 |
| business_type | 業務類型 |
| ref_id | 業務來源 ID |
| idempotency_key | 冪等 key |
| before / after | 變更前後餘額 |

---

## 資產規則

```text
任何業務不得直接 update balance。
必須透過 ledger service 操作資產。
所有資產操作必須寫 ledger_journal。
所有資產操作必須具備 idempotency key。
可用與凍結必須分離。
現貨、合約、槓桿帳戶必須隔離。
除明確負債欄位外，不允許資產負數。
Matching Engine / C++ Core 不得直接改 ledger，只能發出 order / trade / orderbook events。
Settlement / Ledger Service 負責資產結算與資產流水。
所有 C++ Core 輸出事件必須包含 `event_id`、`sequence`、`symbol`、`timestamp`，並支援 replay、reconciliation、compensation。
C++ Core 不得直接修改 `user_balance`、`ledger_journal`、`wallet`、`withdraw`、`admin adjustment`。
```

---

## API / Service 介面

| 介面 | 說明 |
|---|---|
| 查帳戶摘要 | 查現貨、合約、槓桿 |
| 查單一資產 | 查某帳戶某幣種 |
| 入帳 | 增加可用 |
| 扣帳 | 扣可用 |
| 凍結 | 可用轉凍結 |
| 解凍 | 凍結轉可用 |
| 扣凍結 | 從凍結扣除 |
| 帳戶劃轉 | 帳戶間轉移 |
| 查流水 | 查 ledger |
| 建立快照 | 每日資產快照 |
| SettlementService | 接收成交 / funding / liquidation 結果並落帳 |

---

## 不做範圍

```text
不要實作撮合。
不要實作鏈上掃描。
不要實作強平。
不要實作做市策略。
本任務只做帳本能力與介面。
本任務只做 interface / skeleton / TODO，不做 production 級公式。
TODO: requires high-reasoning review before production use
```

---

## 驗收標準

```text
可以建立現貨、合約、槓桿帳戶。
可以查詢帳戶資產摘要。
可以入帳、扣帳、凍結、解凍、扣凍結。
可以做現貨到合約、合約到現貨、現貨到槓桿、槓桿到現貨劃轉。
所有操作寫 ledger_journal。
同一 idempotency key 不會重複入帳。
不允許非法負數。
提供查詢資產流水 API。
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
