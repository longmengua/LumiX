# 03 React Design System：共用元件與格式化工具

## 任務

建立 React 交易所前端共用元件與工具骨架。  
優先沿用現有 UI；沒有就建立簡潔元件。

---

## 共用元件

| 元件 | 用途 |
|---|---|
| AppLayout | 全站框架 |
| Header | 主導航 |
| Sidebar | 側邊欄 |
| PageHeader | 頁面標題 |
| Card | 資訊卡片 |
| DataTable | 表格 |
| Tabs | 分頁 |
| Modal | 彈窗 |
| ConfirmDialog | 高危操作確認 |
| Toast | 操作提示 |
| Badge | 狀態標籤 |
| LoadingState | 載入狀態 |
| EmptyState | 空狀態 |
| ErrorState | 錯誤狀態 |

---

## 交易所專用元件

| 元件 | 用途 |
|---|---|
| AmountText | 金額格式 |
| PriceText | 價格格式 |
| PnlText | 盈虧格式 |
| KycStatusTag | KYC 狀態 |
| SecurityLevel | 安全等級 |
| RiskRatioBar | 槓桿風險率 |
| MarketPairSelector | 交易對選擇 |
| KlinePanel | K 線容器 |
| OrderBook | 深度 |
| TradeTape | 最新成交 |
| OrderEntryPanel | 下單面板 |
| PositionTable | 倉位表 |
| ApiKeyTable | API key 表 |
| NotificationList | 通知列表 |

---

## 工具函式

| 工具 | 說明 |
|---|---|
| formatPrice | 價格 |
| formatAmount | 數量 |
| formatCurrency | 金額 |
| formatPercent | 百分比 |
| formatTime | 時間 |
| maskEmail | Email 脫敏 |
| maskPhone | 手機脫敏 |
| maskApiKey | API key 脫敏 |
| maskAddress | 地址脫敏 |

---

## 不做範圍

```text
不要接真實行情。
不要實作完整 K 線圖表。
不要新增大型 UI 套件。
不要重構全站樣式。
```

---

## 驗收標準

```text
共用元件有基本骨架。
交易所專用元件有基本骨架。
格式化工具可用。
脫敏工具可用。
元件可被後續頁面 import。
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
