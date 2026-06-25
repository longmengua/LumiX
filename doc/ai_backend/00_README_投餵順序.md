# Codex Mini 投餵順序總索引

這一組文件是為了讓 Codex mini 用最低成本、最低上下文量，一份一份完成交易所 MVP。

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
