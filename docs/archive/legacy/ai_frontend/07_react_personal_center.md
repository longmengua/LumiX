# 07 React Personal Center：個人中心完整頁面

## 任務

建立 React 個人中心完整頁面群。  
本任務只做畫面、元件、mock service 與 API hook，不做真實交易後端。

---

## 路由

| 頁面 | 路由 |
|---|---|
| 帳戶總覽 | /account |
| 安全中心 | /account/security |
| KYC | /account/kyc |
| 資產摘要 | /account/assets |
| 帳戶劃轉 | /account/transfer |
| API Key | /account/api-keys |
| 通知中心 | /account/notifications |
| 登入紀錄 | /account/login-history |
| 安全操作紀錄 | /account/security-logs |
| 偏好設定 | /account/preferences |

---

## 帳戶總覽卡片

| 卡片 | 內容 |
|---|---|
| Profile | UID、Email、手機、註冊時間 |
| KYC | 狀態、等級、限制 |
| Security | 安全等級、2FA、白名單 |
| Asset | 總估值、現貨、合約、槓桿 |
| Risk | 未完成安全項、風險提示 |

---

## 安全中心

```text
Login Password
Google Authenticator
Email Verification
SMS Verification
Withdrawal Whitelist
Device Management
Security Activity
```

---

## KYC 頁

狀態：

```text
未認證
審核中
已認證
拒絕
需補件
```

需顯示：

```text
KYC 等級
可提現額度
合約權限
槓桿權限
拒絕原因
提交入口 placeholder
```

---

## 資產摘要 Tabs

```text
Spot Account
Futures Account
Margin Account
```

欄位：

```text
Asset
Available
Frozen
Margin Used
Debt
Interest
Equity
Estimated Value
Action
```

---

## API Key 頁

```text
建立 API key
顯示一次 secret
read / spot trade / futures trade / margin trade
IP 白名單
停用
刪除
最近使用
```

---

## 不做範圍

```text
不要實作真實資產變更。
不要實作真實 KYC 上傳。
不要實作真實 API key 後端。
使用 mock service。
```

---

## 驗收標準

```text
所有個人中心子路由可訪問。
Sidebar 可切換。
帳戶總覽顯示 UID、KYC、安全、資產摘要。
安全中心顯示各安全項狀態。
KYC 顯示狀態與限制。
資產摘要分現貨、合約、槓桿。
API Key 頁有建立、停用、刪除 UI。
所有頁面有 loading、empty、error。
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
