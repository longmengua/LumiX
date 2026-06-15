# Gmail SMTP 註冊驗證碼設定

這份文件只用於本機 MVP/demo 寄送註冊信箱驗證碼。不要把 Gmail 密碼、App Password 或 SMTP 憑證提交到 Git。

## 建立 Gmail App Password

1. 打開 Google 帳號安全性：`https://myaccount.google.com/security`
2. 如果尚未開啟，先開啟 `兩步驟驗證`。
3. 打開 App Passwords：`https://myaccount.google.com/apppasswords`
4. 建立一組名稱為 `match-hub smtp` 的 app password。
5. 複製 Google 產生的 app password，並保存到本機 secret store。

不要使用 Gmail 帳號登入密碼。Gmail SMTP 應使用 app password 或 OAuth。

## 必要環境變數

```bash
CUSTOMER_AUTH_EMAIL_VERIFICATION_ENABLED=true
CUSTOMER_AUTH_EMAIL_SMTP_ENABLED=true
CUSTOMER_AUTH_EMAIL_SMTP_HOST=smtp.gmail.com
CUSTOMER_AUTH_EMAIL_SMTP_PORT=587
CUSTOMER_AUTH_EMAIL_SMTP_USERNAME=your-gmail-address@gmail.com
CUSTOMER_AUTH_EMAIL_SMTP_PASSWORD=your-gmail-app-password
CUSTOMER_AUTH_EMAIL_SMTP_FROM=your-gmail-address@gmail.com
CUSTOMER_AUTH_EMAIL_SMTP_AUTH=true
CUSTOMER_AUTH_EMAIL_SMTP_START_TLS=true
CUSTOMER_AUTH_EMAIL_SMTP_SSL=false
```

## 本機啟動範例

請從 shell 或 secret manager 注入環境變數。避免把 raw secret 留在 shell history。

```bash
CUSTOMER_AUTH_EMAIL_VERIFICATION_ENABLED=true \
CUSTOMER_AUTH_EMAIL_SMTP_ENABLED=true \
CUSTOMER_AUTH_EMAIL_SMTP_HOST=smtp.gmail.com \
CUSTOMER_AUTH_EMAIL_SMTP_PORT=587 \
CUSTOMER_AUTH_EMAIL_SMTP_USERNAME=your-gmail-address@gmail.com \
CUSTOMER_AUTH_EMAIL_SMTP_PASSWORD=your-gmail-app-password \
CUSTOMER_AUTH_EMAIL_SMTP_FROM=your-gmail-address@gmail.com \
CUSTOMER_AUTH_EMAIL_SMTP_AUTH=true \
CUSTOMER_AUTH_EMAIL_SMTP_START_TLS=true \
CUSTOMER_AUTH_EMAIL_SMTP_SSL=false \
./mvnw spring-boot:run
```

## 預期註冊流程

1. 前台先讀 `/api/auth/config`。
2. 如果 email verification 開啟，註冊會建立 pending request。
3. 信件寄出六位數驗證碼，這是主要驗證方式。
4. 使用者在 profile drawer 輸入驗證碼。
5. 郵件連結只作備案流程。

如果 `CUSTOMER_AUTH_EMAIL_SMTP_ENABLED=false`，app 不會寄信，只會在 Spring application log 印出驗證碼，供本機 demo 使用。

## 多國語系信件樣板

驗證信會使用前台註冊當下保存的語言。系統內建 `en`、`zh-TW`、`ms`、`ko` 四種文案。

正式環境可以用設定覆蓋信件樣板，不需要改 code。Body 支援 `{code}`、`{verificationUrl}`、`{expiresAt}` 三個 placeholder。

```bash
CUSTOMER_AUTH_EMAIL_VERIFICATION_TEMPLATES_ZH_TW_SUBJECT="註冊驗證碼"
CUSTOMER_AUTH_EMAIL_VERIFICATION_TEMPLATES_ZH_TW_BODY="你的驗證碼是 {code}\n備援連結：{verificationUrl}\n到期時間：{expiresAt}"
CUSTOMER_AUTH_EMAIL_VERIFICATION_TEMPLATES_EN_SUBJECT="Registration verification code"
CUSTOMER_AUTH_EMAIL_VERIFICATION_TEMPLATES_EN_BODY="Your verification code is {code}\nBackup link: {verificationUrl}\nExpires at: {expiresAt}"
```
