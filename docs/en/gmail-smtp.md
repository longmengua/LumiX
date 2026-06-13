# Gmail SMTP for Registration Email Codes

Use this only for local MVP/demo email delivery. Do not commit Gmail passwords, app passwords, or SMTP credentials to Git.

## Create a Gmail App Password

1. Open Google Account Security: `https://myaccount.google.com/security`
2. Enable `2-Step Verification` if it is not already enabled.
3. Open App Passwords: `https://myaccount.google.com/apppasswords`
4. Create an app password named `match-hub smtp`.
5. Copy the generated app password once and store it in a local secret store.

Do not use the Gmail account login password. Gmail SMTP should use an app password or OAuth.

## Required Environment Variables

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

## Local Run Example

Use environment variables from your shell or secret manager. Avoid commands that remain in shell history with raw secrets.

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

## Expected Registration Flow

1. The frontend reads `/api/auth/config`.
2. If email verification is enabled, registration creates a pending request.
3. The email sends a six-digit code as the primary verification path. Built-in email copy renders the code as a large highlighted HTML block with a plain-text fallback.
4. The frontend includes the browser IANA time zone, so the email expiry is rendered in the customer's local time down to minutes only.
5. The user enters the code in the profile drawer.
6. The email link is only a backup path.

If `CUSTOMER_AUTH_EMAIL_SMTP_ENABLED=false`, the app does not send email. It logs the verification code in the Spring application logs for local demos.

## Localized Email Templates

Verification emails use the registration language saved by the frontend. Built-in templates exist for `en`, `zh-TW`, `ms`, and `ko`, and highlight `{code}` as the main visual element.

Override any template from configuration without code changes. Body placeholders are `{code}`, `{verificationUrl}`, and `{expiresAt}`. `{expiresAt}` is already localized from the request time zone and does not include seconds.

```bash
CUSTOMER_AUTH_EMAIL_VERIFICATION_TEMPLATES_ZH_TW_SUBJECT="註冊驗證碼"
CUSTOMER_AUTH_EMAIL_VERIFICATION_TEMPLATES_ZH_TW_BODY="你的驗證碼是 {code}\n備援連結：{verificationUrl}\n到期時間：{expiresAt}"
CUSTOMER_AUTH_EMAIL_VERIFICATION_TEMPLATES_EN_SUBJECT="Registration verification code"
CUSTOMER_AUTH_EMAIL_VERIFICATION_TEMPLATES_EN_BODY="Your verification code is {code}\nBackup link: {verificationUrl}\nExpires at: {expiresAt}"
```
