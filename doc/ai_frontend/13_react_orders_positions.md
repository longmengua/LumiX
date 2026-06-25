# 13 React Orders / Positions：訂單中心與倉位中心

## 任務

建立 React 訂單中心與倉位中心頁。

---

## 路由

| 頁面 | 路由 |
|---|---|
| 訂單中心 | /orders |
| 現貨訂單 | /orders/spot |
| 合約訂單 | /orders/futures |
| 槓桿訂單 | /orders/margin |
| 成交紀錄 | /orders/trades |
| 倉位中心 | /positions |
| 強平紀錄 | /positions/liquidations |
| 資金費紀錄 | /positions/funding |

---

## 訂單 Tabs

```text
Spot Open Orders
Spot Order History
Spot Trade History
Futures Open Orders
Futures Order History
Futures Trade History
Margin Orders
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
Realized PnL
ROE
Action
```

---

## 操作

```text
撤單
批量撤單
平倉
追加保證金
查看詳情
```

---

## 驗收標準

```text
訂單中心可訪問。
可以切換現貨、合約、槓桿。
表格支援搜尋、狀態篩選、時間篩選。
倉位中心可顯示合約倉位。
強平紀錄可顯示。
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
