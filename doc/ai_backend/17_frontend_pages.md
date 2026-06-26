# 17 Frontend Pages：前端頁面骨架

## 任務

建立交易所前端頁面骨架（OL 前開發期）。  
本任務只建立頁面、路由、布局、API hook 或 mock service，不實作完整交易邏輯。

---

## 頁面清單

| 頁面 | 說明 |
|---|---|
| 首頁行情 | 現貨與合約行情 |
| 註冊 / 登入 | 用戶入口 |
| 個人中心 | 帳戶、安全、KYC、API key |
| 資產總覽 | 現貨、合約、槓桿 |
| 充值 | 地址與紀錄 |
| 提現 | 申請與紀錄 |
| 現貨交易頁 | K 線、深度、下單、委託 |
| 合約交易頁 | K 線、深度、下單、倉位 |
| 槓桿交易頁 | 借幣、還款、交易、風險率 |
| 訂單中心 | 現貨、合約、槓桿訂單 |
| 倉位中心 | 合約持倉 |
| API key 管理 | key、權限、IP |
| 費率頁 | 現貨、合約、槓桿、提幣 |
| 後台入口 | admin |

---

## 合約交易頁布局

```text
Header
  ↓
Contract Selector / Mark Price / Index Price / Funding Rate
  ↓
K Line + Order Book + Order Panel
  ↓
Positions
  ↓
Open Orders
  ↓
Trade History / Funding History
```

---

## 槓桿交易頁布局

```text
Margin Account Summary
  ↓
Borrow / Repay Panel
  ↓
K Line + Order Book + Trade Panel
  ↓
Open Orders
  ↓
Borrow History / Repay History / Interest History
```

---

## 個人中心布局

```text
Sidebar
  ├─ Account Overview
  ├─ Security
  ├─ KYC
  ├─ Assets
  ├─ Transfer
  ├─ Orders
  ├─ Positions
  ├─ Margin Borrow
  ├─ API Keys
  ├─ Notifications
  └─ Preferences
```

---

## 前端要求

```text
優先沿用現有 UI component。
資料可先使用 mock service。
不要硬編寫大型狀態管理，除非 repo 已有。
所有頁面需有 loading、empty、error 狀態。
資產數字需格式化。
敏感資訊需脫敏。
```

---

## 不做範圍

```text
不要實作真實撮合。
不要實作真實 WebSocket 高頻更新。
不要實作複雜圖表，可先留 K 線容器。
不要新增大型 UI 套件，除非專案已使用。
```

---

## 驗收標準

```text
主要頁面路由可訪問。
個人中心可展示核心區塊。
現貨交易頁有 K 線、深度、下單、委託區。
合約交易頁有標記價格、資金費率、倉位區。
槓桿交易頁有借幣、還款、風險率區。
所有頁面有基本 loading / empty / error 狀態。
```

---

## Codex 回覆格式

請依照以下格式回覆：

```text
完成摘要：
- ...

修改檔案：
- ...

新增檔案：
- ...

測試方式：
- ...

尚未完成 TODO：
- ...

風險與注意事項：
- ...
```
