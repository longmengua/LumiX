# 10 React Spot Trading Page：現貨交易頁

## 任務

建立 React 現貨交易頁。  
可以在開發期使用 mock order book、mock trades、mock orders；OL 前必須改接真實 order book / trade feed / orders API，不可把 mock 當正式流程。

---

## 路由

```text
/spot/BTCUSDT
/spot/ETHUSDT
/spot/SOLUSDT
```

---

## 版面

```text
Header
  ↓
Symbol Header: Last Price / 24h Change / 24h Volume
  ↓
Kline + OrderBook + OrderPanel
  ↓
Open Orders
  ↓
Order History / Trade History
```

---

## 元件

| 元件 | 說明 |
|---|---|
| MarketPairSelector | 切換交易對 |
| KlinePanel | K 線容器 |
| OrderBook | 深度 |
| TradeTape | 最新成交 |
| SpotOrderPanel | 買入 / 賣出 |
| OpenOrdersTable | 當前委託 |
| OrderHistoryTable | 歷史訂單 |
| TradeHistoryTable | 成交紀錄 |

---

## 下單面板

```text
Buy / Sell
Limit / Market
Price
Amount
Total
Available
Fee Estimate
Submit
```

---

## 驗收標準

```text
現貨頁可訪問。
可切換 symbol。
K 線、深度、成交可顯示。
買入 / 賣出面板可輸入。
表單有基本驗證。
訂單表有 loading、empty、error。
未登入顯示 Login to Trade。
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
