# React 前端畫面 Codex Mini 投餵順序總索引

這組文件專門用來讓 Codex mini 以 **React + TypeScript** 一份一份建立交易所前端畫面。

預設技術方向：

| 項目 | 建議 |
|---|---|
| 前端框架 | React |
| 語言 | TypeScript |
| 建置工具 | Vite 優先；若 repo 已有 React 架構則沿用 |
| 路由 | React Router；若 repo 已有路由則沿用 |
| 狀態管理 | 先用 React hooks / context；不要一開始引入大型狀態庫 |
| UI | 優先沿用現有 UI；沒有就先用乾淨 CSS / Tailwind，如果 repo 已有 |
| API | 先 mock service / adapter |
| 圖表 | K 線先放容器，不急著接圖表庫 |

---

## 投餵原則

```text
一次只做一個頁面群。
資料先用 mock service。
不要接真實撮合。
不要接真實強平。
不要接真實錢包掃鏈。
不要新增大型套件，除非 repo 已經使用。
不要重構無關模組。
所有頁面要有 loading、empty、error 狀態。
交易、資產、API key 等敏感資訊要脫敏或格式化。
```

---

## 建議投餵順序

| 順序 | 文件 | 用途 |
|---:|---|---|
| 1 | 01_react_repo_scan.md | 掃描 React repo，不改檔 |
| 2 | 02_react_app_setup_rules.md | React 專案規則與目錄約定 |
| 3 | 03_react_design_system_components.md | 共用元件與格式化工具 |
| 4 | 04_react_app_shell_routes.md | App Shell、Header、Sidebar、Routes |
| 5 | 05_react_auth_pages.md | 登入、註冊、2FA |
| 6 | 06_react_home_markets.md | 首頁、市場列表 |
| 7 | 07_react_personal_center.md | 個人中心完整頁面 |
| 8 | 08_react_assets_transfer.md | 資產、帳戶劃轉 |
| 9 | 09_react_deposit_withdraw.md | 充值、提現 |
| 10 | 10_react_spot_trading.md | 現貨交易頁 |
| 11 | 11_react_futures_trading.md | 合約交易頁 |
| 12 | 12_react_margin_trading.md | 槓桿交易頁 |
| 13 | 13_react_orders_positions.md | 訂單中心、倉位中心 |
| 14 | 14_react_api_security_notifications.md | API key、安全紀錄、通知 |
| 15 | 15_react_admin_console.md | 後台頁面 |
| 16 | 16_react_responsive_testing.md | 響應式、測試、交付檢查 |

---

## 最快可展示版本

```text
01_react_repo_scan.md
02_react_app_setup_rules.md
03_react_design_system_components.md
04_react_app_shell_routes.md
06_react_home_markets.md
07_react_personal_center.md
10_react_spot_trading.md
11_react_futures_trading.md
```

---

## 每次丟給 Codex 前加這句

```text
請只完成本文件任務。前端固定使用 React + TypeScript，資料可先用 mock service，不要實作其他文件內容，不要重構無關模組。
```
