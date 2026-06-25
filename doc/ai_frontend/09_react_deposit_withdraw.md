# 09 React Deposit / Withdraw：充值與提現頁

## 任務

建立 React 充值、提現、充值紀錄、提現紀錄頁。  
使用 mock wallet service，不接真實鏈。

---

## 路由

| 頁面 | 路由 |
|---|---|
| 充值 | /assets/deposit |
| 提現 | /assets/withdraw |
| 充值紀錄 | /assets/deposit/history |
| 提現紀錄 | /assets/withdraw/history |
| 提現地址 | /assets/withdraw/addresses |

---

## 充值頁欄位

```text
Asset
Network
Deposit Address
QR Code Placeholder
Copy Address
Minimum Deposit
Confirmations Required
Recent Deposits
```

---

## 提現頁欄位

```text
Asset
Network
Withdraw Address
Amount
Available
Fee
Receive Amount
Security Verification
Submit
```

---

## 提現地址管理

```text
新增地址
地址白名單
地址標籤
刪除地址
啟用 / 停用白名單
```

---

## 紀錄欄位

充值：

```text
Time
Asset
Network
Amount
Tx Hash
Confirmations
Status
```

提現：

```text
Time
Asset
Network
Amount
Fee
Receive Amount
Address
Tx Hash
Status
```

---

## 驗收標準

```text
充值頁可選 asset 與 network。
地址可 copy。
提現頁可填地址與數量。
fee 與 receive amount 可顯示。
新增地址使用安全驗證 modal。
紀錄表有 loading、empty、error。
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
