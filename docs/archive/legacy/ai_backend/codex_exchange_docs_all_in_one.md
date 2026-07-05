# Codex Mini 交易所 OL 分批開工文件合集



---

# FILE: 00_README_投餵順序.md

# Codex Mini 投餵順序總索引

這一組文件是為了讓 Codex mini 用最低成本、最低上下文量，一份一份完成交易所 OL 上線架構校準。

C++ Core 是 OL 前必要項，未來程式碼預計放在 `core/` 或 `matching-core/`；Java 21 + Spring Boot 3 只負責業務後端與接入層。

不要一次把全部文件丟給 Codex。  
建議每次只丟一份，等 Codex 完成、測試通過、提交後，再丟下一份。

---

## 投餵原則

```text
一次只做一個模組。
一次只給必要文件。
不要讓 Codex 自由改架構。
不要讓 Codex 一次實作整個交易所。
不要讓 Codex 重構無關程式。
每次都要求列出修改檔案、測試方式、TODO。
```

---

## 建議順序

| 順序 | 文件 | 用途 |
|---:|---|---|
| 1 | 01_repo_scan.md | 先讓 Codex 讀 repo，不改檔 |
| 2 | 02_project_rules_and_boundaries.md | 建立全專案工程規則 |
| 3 | 03_auth_admin_rbac.md | 帳號、登入、後台權限 |
| 4 | 04_personal_center.md | 個人中心、帳戶、API Key、通知 |
| 5 | 05_unified_account_ledger.md | 統一帳戶與資產帳本 |
| 6 | 06_wallet_deposit_withdraw.md | 充值、提現、錢包 Gateway |
| 7 | 07_market_data_price_index.md | 行情、K 線、指數價、標記價 |
| 8 | 08_spot_trading.md | 現貨交易 |
| 9 | 09_futures_trading.md | U 本位永續合約 |
| 10 | 10_liquidation_insurance_fund.md | 強平、保險基金 |
| 11 | 11_margin_trading.md | 槓桿交易、借幣、還款、利息 |
| 12 | 12_open_api.md | Open API、API Key、簽名、限流 |
| 13 | 13_market_maker.md | 外部做市商、內部做市 bot |
| 14 | 14_admin_console.md | 後台營運 |
| 15 | 15_risk_engine.md | 風控與 Kill Switch |
| 16 | 16_reconciliation_jobs.md | 對帳與補償任務 |
| 17 | 17_frontend_pages.md | 前端頁面骨架 |
| 18 | 18_testing_and_go_live.md | 測試、壓測、上線檢查 |

---

## 最小開工方式

如果你想最快看到畫面，先丟：

```text
01_repo_scan.md
02_project_rules_and_boundaries.md
03_auth_admin_rbac.md
04_personal_center.md
```

如果你想最快建立交易所底層，先丟：

```text
01_repo_scan.md
02_project_rules_and_boundaries.md
05_unified_account_ledger.md
06_wallet_deposit_withdraw.md
08_spot_trading.md
```

如果你想讓合約與槓桿可落地，順序不能跳過：

```text
05_unified_account_ledger.md
07_market_data_price_index.md
09_futures_trading.md
10_liquidation_insurance_fund.md
11_margin_trading.md
```

---

## 每次丟給 Codex 前要加的一句

```text
請只完成本文件任務，不要實作其他文件的內容，不要重構無關模組。
```


---

# FILE: 01_repo_scan.md

# 01 Repo Scan：先讀專案，不改檔

## 任務

請掃描目前 repo，輸出專案結構與下一步實作建議。  
本任務只做分析，不修改任何檔案。

---

## 限制

```text
不要修改任何檔案。
不要新增任何檔案。
不要安裝套件。
不要重構。
不要寫程式。
不要猜不存在的架構。
```

---

## 請輸出

| 項目 | 說明 |
|---|---|
| 技術棧 | 前端、後端、資料庫、框架、套件管理器 |
| 目錄結構 | 主要資料夾用途 |
| 前端入口 | app、pages、routes、components 位置 |
| 後端入口 | server、api、controller、route 位置 |
| Auth 位置 | 登入、middleware、session、token |
| DB 位置 | schema、migration、model、repository |
| API service 位置 | 前後端 API 呼叫位置 |
| UI component | 可復用元件 |
| 測試方式 | 專案目前如何測試 |
| 風險 | 目前架構可能阻礙交易所實作的點 |
| 下一步建議 | 建議先從哪個模組開工 |

---

## 驗收標準

```text
能清楚知道專案怎麼啟動。
能清楚知道路由與 API 放在哪。
能清楚知道 DB schema 或 model 放在哪。
能清楚知道下一份文件應該如何落地。
沒有修改任何檔案。
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


---

# FILE: 02_project_rules_and_boundaries.md

# 02 Project Rules：全專案工程規則與邊界

## 任務

建立交易所 OL 上線架構的工程規則文件與必要的基礎約束。  
如果 repo 已有 docs 或 architecture 目錄，請放在既有位置；否則建立 docs 目錄。

---

## 產品範圍

OL 前必要模組包含：

```text
現貨交易
U 本位永續合約
槓桿交易
充值提現
Open API
外部做市商
內部做市商
後台營運
風控系統
強平系統
對帳系統
```

---

## 不在本任務實作的內容

```text
不要實作撮合。
不要實作強平。
不要實作錢包掃鏈。
不要實作前端完整頁面。
不要實作做市策略。
不要改動大量無關檔案。
```

---

## 必須建立的規則文件

| 文件 | 內容 |
|---|---|
| docs/PROJECT_RULES.md | 全專案工程規則 |
| docs/DOMAIN_BOUNDARIES.md | 模組邊界 |
| docs/SECURITY_RULES.md | 安全與敏感操作規則 |
| docs/ASSET_RULES.md | 資產與帳本規則 |
| docs/CODEX_TASK_RULES.md | 後續 Codex 任務規則 |

---

## 核心工程規則

```text
所有資產變動必須通過帳本服務。
任何業務模組不得直接修改餘額。
現貨、合約、槓桿帳戶必須隔離。
所有敏感操作必須預留二次驗證。
所有後台敏感操作必須寫 operation log。
所有 API key 操作必須寫 security log。
所有充值 callback、提現、成交結算必須具備冪等設計。
合約必須使用指數價格與標記價格。
合約必須有強平與保險基金設計。
槓桿必須有借款、還款、利息、風險率與強平設計。
Open API 必須有簽名、timestamp、IP 白名單與 rate limit。
內部做市商與外部做市商都必須走 Open API。
```

---

## 模組邊界

| 模組 | 負責 |
|---|---|
| User | 用戶、登入、安全、KYC 狀態 |
| Admin | 後台、RBAC、審批、操作日誌 |
| Asset | 統一帳戶、資產、流水、劃轉 |
| Wallet | 充值、提現、掃鏈、callback |
| Spot | 現貨訂單、撮合接入、成交、結算 |
| Futures | 合約訂單、倉位、保證金、資金費率 |
| Margin | 借幣、還款、利息、負債、風險率 |
| Market Data | 深度、成交、ticker、K 線 |
| Price Index | 外部價格、指數價、標記價 |
| Risk | 限額、黑白名單、風控規則 |
| Liquidation | 合約與槓桿強平 |
| Open API | API key、簽名、限流、IP 白名單 |
| Market Maker | 做市商帳號、報表、內部 MM |
| Reconcile | 對帳、快照、補償 |

---

## 驗收標準

```text
已建立工程規則文件。
文件中清楚說明資產不得直接修改。
文件中清楚說明現貨、合約、槓桿帳戶隔離。
文件中清楚說明敏感操作、API key、後台操作的安全要求。
沒有實作無關業務功能。
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


---

# FILE: 03_auth_admin_rbac.md

# 03 Auth / Admin / RBAC：帳號、登入、後台權限

## 任務

建立用戶登入、安全基礎、後台管理員、RBAC 權限與操作日誌骨架。

---

## 實作範圍

### 用戶側

| 功能 | 說明 |
|---|---|
| 註冊 | Email 或手機註冊，依現有專案能力選擇 |
| 登入 | 帳號密碼登入 |
| 登出 | 清除 session / token |
| 修改密碼 | 需登入 |
| 忘記密碼 | 可先留流程或 stub |
| 2FA 狀態 | 可先建立欄位與狀態 |
| 登入紀錄 | IP、User Agent、時間、結果 |

### 後台側

| 功能 | 說明 |
|---|---|
| Admin 登入 | 管理員登入 |
| Admin 使用者 | 後台管理員帳號 |
| 角色 | 建立角色 |
| 權限 | 權限 key |
| 角色權限 | 綁定角色與權限 |
| 操作日誌 | 所有後台操作寫 log |

---

## 不做範圍

```text
不要實作完整 KYC。
不要實作充值提現。
不要實作交易。
不要實作資產帳本。
不要實作完整 Google Authenticator，可先做狀態與介面。
```

---

## 資料模型需求

| 表 / Model | 說明 |
|---|---|
| user | 用戶基本資料 |
| user_security | 用戶安全狀態 |
| user_login_history | 登入紀錄 |
| admin_user | 後台管理員 |
| admin_role | 後台角色 |
| admin_permission | 後台權限 |
| admin_role_permission | 角色權限關聯 |
| admin_operation_log | 後台操作紀錄 |

---

## API 需求

| API | 說明 |
|---|---|
| 註冊 | 建立用戶 |
| 登入 | 回傳 session / token |
| 登出 | 清除登入 |
| 查個人基本資料 | 回傳 UID、Email、手機、安全狀態 |
| 修改密碼 | 預留二次驗證 |
| 查登入紀錄 | 顯示最近登入 |
| Admin 登入 | 後台登入 |
| 查 Admin 角色 | RBAC |
| 建立角色 | RBAC |
| 綁定權限 | RBAC |
| 查操作日誌 | 後台審計 |

---

## 安全要求

```text
密碼不得明文儲存。
登入失敗需要有基本限制或 TODO。
敏感操作需要預留二次驗證。
後台所有寫操作需要 operation log。
不要在 response 中輸出敏感 token、secret、password hash。
```

---

## 驗收標準

```text
用戶可以註冊、登入、登出。
可以查詢個人基本資料。
可以查詢登入紀錄。
Admin 可以登入。
Admin 有角色與權限資料結構。
後台操作會寫 operation log。
所有敏感欄位不明文輸出。
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


---

# FILE: 04_personal_center.md

# 04 Personal Center：個人中心與帳戶入口

## 任務

建立個人中心模組的 API 與前端頁面骨架。  
本任務重點是「可展示、可串接、邊界清楚」，不要實作交易撮合、強平或錢包掃鏈。

---

## 頁面範圍

```text
個人中心
  ├─ 帳戶總覽
  ├─ 安全中心
  ├─ 身分認證 / KYC
  ├─ 資產帳戶
  ├─ 帳戶劃轉
  ├─ 訂單中心入口
  ├─ 倉位中心入口
  ├─ 槓桿借貸入口
  ├─ API Key 管理
  ├─ 通知中心
  ├─ 登入與操作紀錄
  └─ 偏好設定
```

---

## 帳戶總覽

需顯示：

| 欄位 | 說明 |
|---|---|
| UID | 用戶唯一 ID |
| Email / 手機 | 脫敏顯示 |
| KYC 狀態 | 未認證、審核中、已認證、拒絕 |
| 安全等級 | 根據密碼、2FA、白名單等顯示 |
| 總資產估值 | 現貨、合約、槓桿彙總 |
| 風險提醒 | 未 KYC、未 2FA、API key 風險等 |
| 快捷入口 | 充值、提現、劃轉、交易、API key |

---

## 安全中心

| 功能 | OL 要求 |
|---|---|
| 修改密碼 | 預留二次驗證 |
| Google Authenticator | 綁定狀態、啟用狀態 |
| Email 驗證 | 顯示綁定狀態 |
| SMS 驗證 | 顯示綁定狀態 |
| 提現地址白名單 | 顯示是否啟用 |
| 登入設備 | 顯示最近設備 |
| 安全操作紀錄 | 顯示密碼、2FA、API、地址變更 |

---

## KYC

| 功能 | OL 要求 |
|---|---|
| KYC 狀態顯示 | 必要 |
| KYC 等級 | 建議 |
| 提交資料入口 | 可先 stub |
| 審核結果 | 通過、拒絕、需補件 |
| 限制說明 | 不同 KYC 對應提現與交易限制 |

---

## 資產帳戶

需分開顯示：

| 帳戶 | 顯示 |
|---|---|
| 現貨帳戶 | 幣種、可用、凍結、估值 |
| 合約帳戶 | USDT 保證金、可用、占用、未實現盈虧 |
| 槓桿帳戶 | 資產、負債、利息、淨資產、風險率 |

---

## 帳戶劃轉

需要支援：

| 方向 | OL |
|---|---|
| 現貨 → 合約 | 必要 |
| 合約 → 現貨 | 必要 |
| 現貨 → 槓桿 | 必要 |
| 槓桿 → 現貨 | 必要 |
| 合約 ↔ 槓桿 | 可先不做 |

流程：

```text
選來源帳戶
  ↓
選目標帳戶
  ↓
選資產與數量
  ↓
檢查來源可用餘額
  ↓
呼叫帳本服務劃轉介面
  ↓
寫入劃轉紀錄
  ↓
更新前台資產摘要
```

---

## API Key 管理

| 功能 | OL 要求 |
|---|---|
| 建立 API key | 必要 |
| secret 只顯示一次 | 必要 |
| read 權限 | 必要 |
| trade 權限 | 必要 |
| withdraw 權限 | 預設不開 |
| IP 白名單 | 必要 |
| 停用 API key | 必要 |
| 刪除 API key | 必要 |
| 最近使用紀錄 | 建議 |

安全要求：

```text
建立、修改、刪除 API key 需要預留 2FA。
API secret 不可明文持久化。
列表不可顯示 secret。
withdraw 權限預設不可用。
```

---

## 通知中心

| 類型 | 說明 |
|---|---|
| 登入通知 | 新 IP、新設備 |
| 安全通知 | 修改密碼、2FA、API key |
| 充值通知 | 充值成功 |
| 提現通知 | 申請、審核、成功、失敗 |
| 交易通知 | 成交、撤單 |
| 合約通知 | 接近強平、強平完成、資金費率 |
| 槓桿通知 | 風險率預警、利息、強平 |
| 系統通知 | 維護、暫停、恢復 |

---

## API 範圍

| API | 說明 |
|---|---|
| 查個人資料 | UID、Email、手機、KYC、安全等級 |
| 查安全狀態 | 密碼、2FA、Email、SMS、白名單 |
| 查 KYC 狀態 | 狀態與限制 |
| 查帳戶摘要 | 現貨、合約、槓桿 |
| 建立帳戶劃轉 | 提交劃轉請求 |
| 查 API key | API key 列表 |
| 建立 API key | 建立並顯示一次 secret |
| 修改 API key | 權限與 IP 白名單 |
| 停用 API key | disable |
| 刪除 API key | delete |
| 查通知 | 通知列表 |
| 標記通知已讀 | read |
| 查登入紀錄 | login history |
| 查安全操作紀錄 | security log |

---

## 不做範圍

```text
不要實作撮合。
不要實作強平。
不要實作錢包掃鏈。
不要實作真實充值入帳。
不要直接修改資產餘額。
資產資料可先 mock 或呼叫 stub service。
```

---

## 驗收標準

```text
個人中心頁面可以正常進入。
帳戶總覽能顯示 UID、KYC、安全等級、資產摘要。
安全中心能顯示密碼、2FA、Email、SMS、提現白名單狀態。
資產總覽能分現貨、合約、槓桿帳戶。
帳戶劃轉頁有來源、目標、資產、數量欄位。
API key 頁可建立、停用、刪除，並支援 IP 白名單欄位。
通知中心能顯示通知列表與已讀狀態。
登入紀錄與安全操作紀錄可以查詢。
所有敏感操作有 TODO 或 hook 預留二次驗證。
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


---

# FILE: 05_unified_account_ledger.md

# 05 Unified Account Ledger：統一帳戶與資產帳本

## 任務

建立交易所核心資產帳本骨架。  
所有現貨、合約、槓桿、錢包、做市商、平台收入都要依賴這套帳本。

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

---

## 不做範圍

```text
不要實作撮合。
不要實作鏈上掃描。
不要實作強平。
不要實作做市策略。
本任務只做帳本能力與介面。
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


---

# FILE: 06_wallet_deposit_withdraw.md

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


---

# FILE: 07_market_data_price_index.md

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


---

# FILE: 08_spot_trading.md

# 08 Spot Trading：現貨交易

## 任務

建立現貨交易模組骨架，包含訂單、撤單、成交、結算、手續費與行情事件。  
如果撮合引擎尚未完成，可先建立 Java 接入層 / stub，但 C++ Core 是 OL 前必要項。

---

## 功能範圍

| 功能 | OL 必要 |
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
透過 Java Order Service / `MatchingEngineClient` / gRPC / event bus 送入 C++ Core
  ↓
成交或掛單
  ↓
成交事件進入 settlement
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
若可撤，通知 Java Order Service，再轉交 C++ Core
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
```

---

## 不做範圍

```text
不要實作合約。
不要實作槓桿借貸。
不要實作內部做市策略。
如果 matching engine 未完成，僅作開發/驗證骨架，OL 前不得作為正式流程，C++ Core 是 OL 前必要項。
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


---

# FILE: 09_futures_trading.md

# 09 Futures Trading：U 本位永續合約

## 任務

建立 U 本位永續合約模組骨架，包含合約訂單、倉位、保證金、盈虧、資金費率與標記價格接入。

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
透過 Java Order Service / `MatchingEngineClient` / gRPC / event bus 送入 C++ Core
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


---

# FILE: 10_liquidation_insurance_fund.md

# 10 Liquidation：強平引擎與保險基金

## 任務

建立合約與槓桿強平系統骨架，包含保證金率、強平價格、強平流程、保險基金與極端風險處理。

---

## 合約強平判斷

```text
標記價格更新
  ↓
重新計算未實現盈虧
  ↓
計算帳戶權益
  ↓
計算維持保證金需求
  ↓
判斷保證金率
  ↓
低於安全線
  ↓
進入強平流程
```

---

## 合約強平流程

```text
觸發強平
  ↓
凍結倉位
  ↓
取消該合約未成交委託
  ↓
釋放可用保證金
  ↓
強平引擎接管倉位
  ↓
部分強平或全部強平
  ↓
計算剩餘資產或穿倉
  ↓
更新保險基金
  ↓
寫強平紀錄
  ↓
推送用戶通知
```

---

## 槓桿強平流程

```text
價格更新
  ↓
重新估算槓桿帳戶資產價值
  ↓
計算負債與利息
  ↓
計算風險率
  ↓
低於強平線
  ↓
取消未成交委託
  ↓
強制賣出資產或買回負債資產
  ↓
償還負債
  ↓
不足部分進入壞帳 / 保險基金
```

---

## 保險基金

| 能力 | 說明 |
|---|---|
| 查餘額 | 查看保險基金資產 |
| 入帳 | 強平剩餘資產、平台注入 |
| 出帳 | 穿倉承接 |
| 流水 | 每次變動寫入 |
| 後台查看 | 營運可查 |
| 對帳 | 每日對帳 |

---

## 風險模式

| 模式 | 說明 |
|---|---|
| 正常模式 | 可開倉、可平倉 |
| 僅減倉 | 禁止開倉，只允許降低風險 |
| 暫停交易 | 禁止下單與撮合 |
| 強平保護 | 限制強平單打穿市場 |
| 極端處理 | 保險基金不足時使用 |

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| liquidation_record | 強平紀錄 |
| liquidation_order | 強平訂單 |
| insurance_fund_journal | 保險基金流水 |
| risk_limit_config | 風險限額 |
| margin_risk_snapshot | 槓桿風險快照 |
| futures_risk_snapshot | 合約風險快照 |

---

## API / 後台需求

| API | 說明 |
|---|---|
| 查強平紀錄 | 用戶與後台 |
| 查風險倉位 | 後台 |
| 查保險基金 | 後台 |
| 設定風險限額 | 後台 |
| 啟用僅減倉 | 後台 |
| 暫停強平或交易 | 後台 emergency |

---

## 不做範圍

```text
不要實作完整高頻強平撮合。
不要實作 ADL 完整排序，可先預留。
不要直接改資產，必須呼叫帳本服務。
```

---

## 驗收標準

```text
可以根據 mark price 計算合約倉位風險。
可以產生強平紀錄。
可以取消被強平用戶的未成交委託。
可以更新保險基金流水。
可以設定僅減倉模式。
可以查詢強平紀錄與保險基金。
所有強平資產變動透過帳本服務。
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


---

# FILE: 11_margin_trading.md

# 11 Margin Trading：槓桿交易、借幣、還款、利息

## 任務

建立槓桿交易模組骨架，包含槓桿帳戶、借幣、還款、利息、負債、風險率與強平接入。

---

## 第一版範圍

| 項目 | OL 必要 |
|---|---|
| 模式 | 現貨槓桿 |
| 交易對 | BTC/USDT、ETH/USDT |
| 借貸資產 | USDT、BTC、ETH |
| 保證金模式 | 逐倉優先 |
| 利息 | 小時計息或日計息 |
| 強平 | 必須接入 |
| 自動借款 | 可第二版 |
| 自動還款 | 可第二版 |

---

## 槓桿帳戶概念

| 概念 | 說明 |
|---|---|
| 資產 | 用戶槓桿帳戶資產 |
| 借入 | 用戶從平台借入 |
| 負債 | 本金 + 利息 |
| 淨資產 | 資產 - 負債 |
| 風險率 | 強平判斷 |
| 利息 | 借款成本 |
| 還款 | 歸還本金與利息 |

---

## 借幣流程

```text
用戶申請借幣
  ↓
檢查抵押資產
  ↓
計算可借額度
  ↓
檢查可借資產池
  ↓
增加槓桿帳戶資產
  ↓
增加負債
  ↓
開始計息
  ↓
寫借款流水
```

---

## 還款流程

```text
用戶申請還款
  ↓
計算本金與利息
  ↓
檢查可用資產
  ↓
扣除還款資產
  ↓
降低負債
  ↓
寫還款流水
  ↓
重新計算風險率
```

---

## 槓桿交易流程

```text
用戶在槓桿帳戶下單
  ↓
檢查槓桿帳戶資產與負債
  ↓
檢查風險率
  ↓
凍結交易資產
  ↓
進入現貨撮合
  ↓
成交後更新槓桿帳戶
  ↓
重新計算負債與風險率
```

---

## 資料需求

| Model / Table | 說明 |
|---|---|
| margin_account | 槓桿帳戶 |
| margin_borrow_record | 借幣紀錄 |
| margin_repay_record | 還款紀錄 |
| margin_interest_record | 利息紀錄 |
| margin_debt | 負債 |
| margin_risk_snapshot | 風險快照 |
| lending_pool | 可借資產池 |
| margin_liquidation_record | 槓桿強平紀錄 |

---

## API 需求

| API | 說明 |
|---|---|
| 查槓桿帳戶 | 資產、負債、風險率 |
| 借幣 | borrow |
| 還款 | repay |
| 查借款紀錄 | borrow history |
| 查還款紀錄 | repay history |
| 查利息紀錄 | interest history |
| 槓桿下單 | 使用槓桿帳戶 |
| 查強平紀錄 | margin liquidation |

---

## 不做範圍

```text
不要實作自動借款。
不要實作自動還款。
不要實作跨幣種複雜抵押折扣，可先預留。
不要直接修改資產，必須通過帳本服務。
```

---

## 驗收標準

```text
可以查槓桿帳戶。
可以借幣並產生負債。
可以還款並降低負債。
可以計算利息。
可以查借款、還款、利息紀錄。
可以計算風險率。
槓桿交易使用槓桿帳戶資產。
所有資產與負債變動有流水。
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


---

# FILE: 12_open_api.md

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


---

# FILE: 13_market_maker.md

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

| 指標 | OL 指標 |
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


---

# FILE: 14_admin_console.md

# 14 Admin Console：後台營運系統

## 任務

建立交易所後台營運模組。  
後台必須能支援用戶、資產、錢包、現貨、合約、槓桿、風控、做市商、對帳與事故處理。

---

## 後台模組

| 模組 | 功能 |
|---|---|
| 用戶管理 | 查用戶、KYC、凍結、解凍、安全重置 |
| 資產管理 | 查現貨、合約、槓桿帳戶與流水 |
| 錢包管理 | 充值、提現、審核、補單 |
| 現貨管理 | 訂單、成交、交易對、費率 |
| 合約管理 | 倉位、強平、資金費率、槓桿 |
| 槓桿管理 | 借貸、負債、利息、強平 |
| 風控管理 | 規則、黑白名單、限額 |
| 做市商管理 | 帳號、API key、限流、績效 |
| 保險基金 | 餘額、流水、穿倉 |
| 對帳報表 | 每日對帳結果 |
| 操作日誌 | 所有後台操作 |

---

## 事故處理能力

| 能力 | 說明 |
|---|---|
| 暫停現貨交易對 | spot pause |
| 暫停合約交易對 | futures pause |
| 合約僅減倉 | reduce only |
| 暫停槓桿借幣 | margin borrow pause |
| 暫停提現 | withdraw pause |
| 凍結用戶 | user freeze |
| 凍結資產 | asset freeze |
| 停用 API key | api key disable |
| 停用做市商 | MM disable |
| 停止內部 MM | internal MM stop |
| 查對帳異常 | reconciliation mismatch |

---

## 後台 API

| API | 說明 |
|---|---|
| 查用戶 | user list |
| 凍結 / 解凍用戶 | user status |
| 查資產 | balances |
| 查資產流水 | ledger |
| 查充值 | deposits |
| 查提現 | withdraws |
| 審核提現 | approve / reject |
| 查現貨訂單 | spot orders |
| 查合約倉位 | futures positions |
| 查槓桿負債 | margin debts |
| 查強平紀錄 | liquidations |
| 查做市商 | market makers |
| 查對帳結果 | reconciliation |
| 執行 kill switch | emergency |
| 查操作日誌 | operation logs |

---

## 安全要求

```text
後台所有寫操作必須驗證權限。
後台所有敏感操作必須寫 operation log。
高危操作需要二次確認或審批。
不得在後台直接改餘額。
人工調帳必須走審批與帳本服務。
```

---

## 不做範圍

```text
不要在後台直接實作交易邏輯。
不要在後台直接改資料庫資產。
不要跳過 RBAC。
```

---

## 驗收標準

```text
後台可查用戶、資產、流水。
後台可查充值與提現。
後台可審核提現。
後台可查現貨訂單與成交。
後台可查合約倉位與強平。
後台可查槓桿借貸與負債。
後台可查做市商。
後台可執行基礎 kill switch。
所有後台寫操作都有 operation log。
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


---

# FILE: 15_risk_engine.md

# 15 Risk Engine：風控與 Kill Switch

## 任務

建立交易所風控模組骨架，包含帳號、提現、現貨、合約、槓桿、API、做市商與系統級 Kill Switch。

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


---

# FILE: 16_reconciliation_jobs.md

# 16 Reconciliation Jobs：對帳與補償任務

## 任務

建立每日對帳與補償任務骨架。  
交易所上線前必須能對資產、錢包、訂單、成交、倉位、借貸、費用與保險基金。

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


---

# FILE: 17_frontend_pages.md

# 17 Frontend Pages：前端頁面骨架

## 任務

建立交易所前端頁面骨架（OL 前開發期）。  
本任務只建立頁面、路由、布局、API hook 或 mock service，不實作完整交易邏輯。

---

## 頁面清單

| 頁面 | 說明 |
|---|---|
| 首頁行情 | 現貨與合約行情 |
| 註冊 / 登入 | 用戶入口 |
| 個人中心 | 帳戶、安全、KYC、API key |
| 資產總覽 | 現貨、合約、槓桿 |
| 充值 | 地址與紀錄 |
| 提現 | 申請與紀錄 |
| 現貨交易頁 | K 線、深度、下單、委託 |
| 合約交易頁 | K 線、深度、下單、倉位 |
| 槓桿交易頁 | 借幣、還款、交易、風險率 |
| 訂單中心 | 現貨、合約、槓桿訂單 |
| 倉位中心 | 合約持倉 |
| API key 管理 | key、權限、IP |
| 費率頁 | 現貨、合約、槓桿、提幣 |
| 後台入口 | admin |

---

## 合約交易頁布局

```text
Header
  ↓
Contract Selector / Mark Price / Index Price / Funding Rate
  ↓
K Line + Order Book + Order Panel
  ↓
Positions
  ↓
Open Orders
  ↓
Trade History / Funding History
```

---

## 槓桿交易頁布局

```text
Margin Account Summary
  ↓
Borrow / Repay Panel
  ↓
K Line + Order Book + Trade Panel
  ↓
Open Orders
  ↓
Borrow History / Repay History / Interest History
```

---

## 個人中心布局

```text
Sidebar
  ├─ Account Overview
  ├─ Security
  ├─ KYC
  ├─ Assets
  ├─ Transfer
  ├─ Orders
  ├─ Positions
  ├─ Margin Borrow
  ├─ API Keys
  ├─ Notifications
  └─ Preferences
```

---

## 前端要求

```text
優先沿用現有 UI component。
資料可先使用 mock service。
不要硬編寫大型狀態管理，除非 repo 已有。
所有頁面需有 loading、empty、error 狀態。
資產數字需格式化。
敏感資訊需脫敏。
```

---

## 不做範圍

```text
不要實作真實撮合。
不要實作真實 WebSocket 高頻更新。
不要實作複雜圖表，可先留 K 線容器。
不要新增大型 UI 套件，除非專案已使用。
```

---

## 驗收標準

```text
主要頁面路由可訪問。
個人中心可展示核心區塊。
現貨交易頁有 K 線、深度、下單、委託區。
合約交易頁有標記價格、資金費率、倉位區。
槓桿交易頁有借幣、還款、風險率區。
所有頁面有基本 loading / empty / error 狀態。
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


---

# FILE: 18_testing_and_go_live.md

# 18 Testing & Go Live：測試、壓測、上線檢查

## 任務

建立交易所 OL 的測試清單、驗收標準、壓測項目與上線檢查表。  
本任務可先建立 docs、test plan、stub tests，不需要一次補齊所有測試。

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

## 必測場景

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
核心帳本測試至少有 stub 或 TODO。
合約與槓桿風險測試有明確場景。
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
