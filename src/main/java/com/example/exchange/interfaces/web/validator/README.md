# interfaces/web/validator

Bean Validation extensions。

目前內容：
- `ValidSymbol` annotation。
- `ValidSymbolValidator`。

目前狀態：
- 用於 REST request 的 symbol 基礎驗證。

注意：
- Validation 只做輸入格式與輕量查核；交易規則仍交給 application/domain risk checks。
