# 06 React Home / Markets：首頁與市場列表

## 任務

建立 React 首頁與市場列表頁。  
可以在開發期使用 mock market service；OL 前必須改接真實 market API / WebSocket。

---

## 首頁區塊

```text
Hero
熱門交易對
現貨市場
合約市場
產品入口
新手入口
Footer
```

---

## 市場頁

Tabs：

```text
Spot
Futures
Margin
Favorites
```

市場表格欄位：

```text
Symbol
Last Price
24h Change
24h High
24h Low
24h Volume
Action
```

---

## 初始 mock symbols

```text
BTC/USDT
ETH/USDT
SOL/USDT
BTCUSDT Perp
ETHUSDT Perp
SOLUSDT Perp
```

---

## 操作

```text
搜尋 symbol
切換市場類型
收藏，可 local state
點 Trade 跳轉交易頁
```

---

## 驗收標準

```text
首頁可訪問。
市場頁可訪問。
Spot / Futures / Margin 可切換。
表格有 loading、empty、error。
點擊 Trade 跳轉到 /spot/:symbol 或 /futures/:symbol。
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
