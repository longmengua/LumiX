# 12 React Margin Trading Page：槓桿交易頁

## 任務

建立 React 槓桿交易頁。  
使用 mock 槓桿資產、負債、利息、風險率。

---

## 路由

```text
/margin/BTCUSDT
/margin/ETHUSDT
```

---

## 版面

```text
Margin Account Summary
  ↓
Borrow / Repay Panel
  ↓
Kline + OrderBook + Trade Panel
  ↓
Open Orders
  ↓
Borrow History / Repay History / Interest History
```

---

## 元件

| 元件 | 說明 |
|---|---|
| MarginSummaryCard | 資產、負債、利息 |
| RiskRatioBar | 風險率 |
| BorrowPanel | 借幣 |
| RepayPanel | 還款 |
| MarginOrderPanel | 槓桿買賣 |
| BorrowHistoryTable | 借款紀錄 |
| RepayHistoryTable | 還款紀錄 |
| InterestHistoryTable | 利息紀錄 |

---

## 風險狀態

```text
Safe
Warning
Danger
Liquidation
```

---

## 驗收標準

```text
槓桿頁可訪問。
顯示資產、負債、利息、風險率。
借幣與還款表單可顯示。
槓桿交易面板可顯示。
歷史紀錄表可顯示。
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
