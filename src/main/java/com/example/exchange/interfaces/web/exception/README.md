# interfaces/web/exception

Web exception 與錯誤回應模型。

目前內容：
- `BusinessException`、`BusinessErrorCode`。
- `ErrorResponse`。
- `GlobalExceptionHandler`。

注意：
- 新增業務錯誤時要用穩定 error code，避免前端只能解析 message。
- 高風險錯誤不要把 secret、private key、raw auth header 打到 response。
