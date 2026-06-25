# 16 React Responsive / Testing：響應式、狀態、測試、交付

## 任務

建立 React 前端響應式規則、狀態處理、測試與交付檢查。

---

## 全站狀態

```text
loading
empty
error
success
disabled
unauthorized
forbidden
maintenance
```

---

## 格式化

| 類型 | 規則 |
|---|---|
| 價格 | 根據 symbol 精度 |
| 數量 | 根據 asset 精度 |
| 金額 | 千分位與 USDT |
| PnL | 正負清楚 |
| 百分比 | 兩位小數 |
| 時間 | YYYY-MM-DD HH:mm:ss |
| API Key | 脫敏 |
| 地址 | 前後截斷 |

---

## 響應式

| 裝置 | 規則 |
|---|---|
| Desktop | 完整交易布局 |
| Tablet | 表格與 order book 壓縮 |
| Mobile | Tabs 堆疊 |
| Admin | Desktop 優先 |

Mobile 交易頁：

```text
Symbol Header
Tabs: Kline / OrderBook / Trades
Order Panel
Positions / Orders
```

---

## 必測路由

```text
/
 /markets
/login
/register
/account
/account/security
/account/api-keys
/assets
/assets/transfer
/assets/deposit
/assets/withdraw
/spot/BTCUSDT
/futures/BTCUSDT
/margin/BTCUSDT
/orders
/positions
/admin
/admin/risk
```

---

## 交付標準

```text
主要頁面可訪問。
沒有明顯 console error。
沒有 TypeScript error。
主要表單有驗證。
主要表格有 loading / empty / error。
敏感資訊已脫敏。
高危操作有二次確認。
```

---

## 驗收標準

```text
有前端 QA checklist。
主要頁面符合基本響應式。
格式化工具被主要頁面使用。
敏感資訊有脫敏。
可執行 lint / typecheck / test 或有 TODO。
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
