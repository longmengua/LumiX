# 04 React App Shell：主框架、Header、Sidebar、Routes

## 任務

建立 React App 主框架與路由。  
使用 React Router 或 repo 現有路由方案。

---

## 主路由

| 頁面 | 路由 |
|---|---|
| 首頁 | / |
| 市場 | /markets |
| 現貨 | /spot/:symbol |
| 合約 | /futures/:symbol |
| 槓桿 | /margin/:symbol |
| 資產 | /assets |
| 訂單 | /orders |
| 倉位 | /positions |
| 個人中心 | /account |
| 後台 | /admin |

---

## Header

```text
Logo
Markets
Spot
Futures
Margin
Assets
Orders
Account / Login
```

---

## Account Sidebar

```text
Overview
Security
KYC
Assets
Transfer
Deposit
Withdraw
API Keys
Notifications
Login History
Security Logs
Preferences
```

---

## Admin Sidebar

```text
Dashboard
Users
Assets
Wallet
Spot
Futures
Margin
Risk
Market Makers
Insurance Fund
Reconciliation
Operation Logs
Settings
```

---

## Auth 狀態

| 狀態 | 顯示 |
|---|---|
| 未登入 | Login / Register |
| 已登入 | Assets / Orders / Account |
| Admin | Admin entrance |
| 無權限 | Forbidden |
| 載入中 | Skeleton |

---

## 不做範圍

```text
不要實作完整 auth。
不要實作完整頁面內容。
不要改後端。
```

---

## 驗收標準

```text
React routes 可正常切換。
主 Header 可顯示。
Account sidebar 可顯示。
Admin sidebar 可顯示。
主要路由都有 placeholder。
未登入與已登入狀態有基礎 UI 或 TODO。
```

---

## Codex 回覆格式

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

主要 React 元件：
- ...

API / Mock：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

注意事項：
- ...
```
