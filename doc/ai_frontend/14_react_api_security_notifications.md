# 14 React API Key / Security / Notifications：API、安全紀錄、通知

## 任務

建立 React API Key 管理、安全操作紀錄、登入紀錄、通知中心。

---

## 路由

| 頁面 | 路由 |
|---|---|
| API Key | /account/api-keys |
| 安全紀錄 | /account/security-logs |
| 登入紀錄 | /account/login-history |
| 通知中心 | /account/notifications |

---

## API Key 表格

```text
Name
Key Preview
Permissions
IP Whitelist
Created At
Last Used
Status
Action
```

---

## 建立 API Key Modal

```text
Name
Read
Spot Trade
Futures Trade
Margin Trade
IP Whitelist
Security Verification
```

成功後：

```text
顯示 API key
顯示 secret，一次性
提醒保存
```

---

## 安全紀錄

```text
Time
Action
IP
Device
Result
Details
```

---

## 通知分類

```text
All
Security
Deposit
Withdraw
Trade
Futures
Margin
System
```

---

## 驗收標準

```text
API key 列表可顯示。
建立 API key modal 可打開。
secret 只在建立成功畫面顯示。
可以停用 / 刪除 API key UI。
安全紀錄可顯示。
登入紀錄可顯示。
通知可分類與標記已讀。
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
