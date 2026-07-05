# 08 React Assets / Transfer：資產總覽與帳戶劃轉

## 任務

建立 React 資產總覽與帳戶劃轉頁。

---

## 路由

| 頁面 | 路由 |
|---|---|
| 資產總覽 | /assets |
| 現貨帳戶 | /assets/spot |
| 合約帳戶 | /assets/futures |
| 槓桿帳戶 | /assets/margin |
| 帳戶劃轉 | /assets/transfer |
| 資產流水 | /assets/history |

---

## 資產總覽

```text
Total Equity
Spot Value
Futures Equity
Margin Equity
Account Tabs
Asset Table
Recent Asset History
```

---

## 帳戶表格欄位

現貨：

```text
Asset
Available
Frozen
Total
Estimated Value
Action
```

合約：

```text
Asset
Wallet Balance
Available
Margin Used
Unrealized PnL
Equity
Action
```

槓桿：

```text
Asset
Available
Borrowed
Interest
Net Asset
Risk Ratio
Action
```

---

## 劃轉表單

```text
From Account
To Account
Asset
Amount
Available
Max Button
Submit
```

支援：

```text
Spot → Futures
Futures → Spot
Spot → Margin
Margin → Spot
```

---

## 驗收標準

```text
資產總覽可訪問。
三種帳戶可切換。
劃轉表單可輸入。
不可選相同來源與目標。
超過可用餘額顯示錯誤。
提交後顯示成功 / 失敗狀態。
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
