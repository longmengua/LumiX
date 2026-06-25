# 11 React Futures Trading Page：U 本位永續合約頁

## 任務

建立 React 合約交易頁。  
使用 mock 合約行情、mock 倉位、mock 訂單，不實作真實強平。

---

## 路由

```text
/futures/BTCUSDT
/futures/ETHUSDT
/futures/SOLUSDT
```

---

## 版面

```text
Header
  ↓
Contract Header: Last / Mark Price / Index Price / Funding Rate / Countdown
  ↓
Kline + OrderBook + FuturesOrderPanel
  ↓
Positions
  ↓
Open Orders
  ↓
Trade History / Funding History
```

---

## 元件

| 元件 | 說明 |
|---|---|
| ContractSelector | 合約選擇 |
| MarkPriceInfo | 標記價格 |
| IndexPriceInfo | 指數價格 |
| FundingRateInfo | 資金費率 |
| FuturesOrderPanel | 開多 / 開空 / 平倉 |
| LeverageSelector | 槓桿 |
| MarginModeSelector | 逐倉 / 全倉 |
| PositionTable | 倉位 |
| FuturesOrdersTable | 合約委託 |
| FundingHistoryTable | 資金費紀錄 |

---

## 下單面板

```text
Open Long
Open Short
Close
Order Type
Price
Size
Leverage
Margin Mode
Cost
Estimated Liq Price
Fee Estimate
Submit
```

---

## 倉位欄位

```text
Symbol
Side
Size
Entry Price
Mark Price
Liq Price
Margin
Leverage
Unrealized PnL
ROE
Action
```

---

## 驗收標準

```text
合約頁可訪問。
顯示 mark price、index price、funding rate。
可選槓桿。
可選保證金模式。
下單面板有開多、開空、平倉。
倉位表顯示 PnL 與強平價。
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
