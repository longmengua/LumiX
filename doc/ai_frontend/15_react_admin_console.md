# 15 React Admin Console：後台前端頁面

## 任務

建立 React 後台管理頁面骨架。  
使用 mock admin service。

---

## 路由

| 頁面 | 路由 |
|---|---|
| Dashboard | /admin |
| Users | /admin/users |
| Assets | /admin/assets |
| Wallet | /admin/wallet |
| Spot | /admin/spot |
| Futures | /admin/futures |
| Margin | /admin/margin |
| Risk | /admin/risk |
| Market Makers | /admin/market-makers |
| Insurance Fund | /admin/insurance-fund |
| Reconciliation | /admin/reconciliation |
| Operation Logs | /admin/operation-logs |
| Settings | /admin/settings |

---

## Dashboard 卡片

```text
Total Users
Daily Volume
Open Withdrawals
Risk Alerts
Active Market Makers
Insurance Fund Balance
Reconciliation Status
System Status
```

---

## 管理頁面

| 頁面 | 需要內容 |
|---|---|
| Users | 用戶表、凍結、解凍、重置 2FA |
| Assets | 資產表、流水入口 |
| Wallet | 充值、提現、審核 |
| Spot | 交易對、訂單、成交、費率 |
| Futures | 合約、倉位、強平、資金費率 |
| Margin | 借貸、負債、利息、強平 |
| Risk | kill switch、規則、黑白名單 |
| Market Makers | 做市商、API、績效 |
| Reconciliation | 對帳結果與差異 |
| Logs | 操作日誌 |

---

## 高危操作

必須使用 ConfirmDialog：

```text
凍結用戶
解凍用戶
審核提現
拒絕提現
暫停交易
僅減倉
停用 API key
停用做市商
停止內部 MM
```

---

## 驗收標準

```text
/admin 可訪問。
後台 sidebar 可切換。
Dashboard 卡片可顯示。
主要後台頁有表格。
Risk 頁有 kill switch UI。
高危操作有 ConfirmDialog。
所有表格有 loading、empty、error。
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
