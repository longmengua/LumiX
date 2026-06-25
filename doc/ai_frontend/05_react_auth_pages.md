# 05 React Auth Pages：登入、註冊、2FA

## 任務

建立 React 認證頁面與安全驗證彈窗。  
可以使用 mock auth service。

---

## 頁面

| 頁面 | 路由 |
|---|---|
| 登入 | /login |
| 註冊 | /register |
| 忘記密碼 | /forgot-password |
| 重置密碼 | /reset-password |
| 2FA 驗證 | /two-factor |

---

## 登入欄位

```text
Email / 手機
密碼
記住我
登入按鈕
忘記密碼
註冊入口
```

---

## 註冊欄位

```text
Email / 手機
驗證碼
密碼
確認密碼
邀請碼，可選
同意條款
```

---

## SecurityVerifyModal

用途：

```text
修改密碼
建立 API key
刪除 API key
新增提現地址
提現
關閉 2FA
```

驗證方式：

```text
Email code
SMS code
Google Authenticator
```

---

## 不做範圍

```text
不要實作真實 auth 後端。
不要保存明文密碼。
不要把 token 寫死。
```

---

## 驗收標準

```text
登入、註冊、忘記密碼、2FA 頁可訪問。
表單有基本驗證。
錯誤狀態可顯示。
SecurityVerifyModal 可復用。
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
