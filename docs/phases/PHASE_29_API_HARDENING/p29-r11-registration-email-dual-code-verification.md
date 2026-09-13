# P29-R11 註冊 Email 雙驗證碼

## 狀態

`IMPLEMENTED_AWAITING_HUMAN_REVIEW`

## 目標與範圍

註冊 CAPTCHA 通過後，先以受控 SMTP 寄送六位數字碼與五位英文字母碼；前端會提供 3 至 5 個五碼字母候選 checkbox，使用者須勾選 Email 所示的正確碼。兩組都通過前，不建立 `users`、`user_credentials`、受信任裝置或 session。

## 安全邊界

- 原始數字碼、五位字母碼與密碼均不回傳 API、不寫入 log；候選字母碼只用於前端比對，Email 內容才告知答案；資料庫只保存兩個 code 的 SHA-256 digest 與 BCrypt password hash。
- 每個 email 同一時間只保留一個待驗證申請；重新申請會原子失效舊信中的 code。
- 驗證採 row lock、constant-time digest 比較與最多五次失敗限制；達上限、過期或已使用都 fail-closed，必須重新通過 CAPTCHA 並寄信。
- SMTP 未啟用時 API 回應 `SERVICE_UNAVAILABLE`，不建立半成品帳號。
- 最終建立時仍由 `users.email` unique constraint 裁決併發重複註冊。

## Migration 與 rollback

`V013__add_registration_email_verification.sql` 僅新增 `registration_verification_requests`，排在既有 V012 後。回滾僅能在確認沒有待驗證註冊需要保留時，停止新註冊流量後刪除該表；不可修改已套用的 V009–V012 migration。此 task 不變更帳本、資產、交易或資金資料。

## 明確未完成

沒有 SMS、寄信佇列、rate-limit/WAF、跨裝置恢復、production SMTP secret 管理或 production launch；本項是 authentication runtime 變更，仍需人工安全審核。

`HUMAN_REVIEW_REQUIRED: yes`
