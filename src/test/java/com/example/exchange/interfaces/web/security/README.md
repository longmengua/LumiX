# test interfaces/web/security

Security helper tests。

目前內容：
- `ApiKeyAuthenticatorTest`。
- `JwtAuthenticatorTest`。
- `IpAllowlistTest`。

注意：
- 認證邏輯改動要保持 secret masking、permission scope、過期/格式錯誤的測試覆蓋。
