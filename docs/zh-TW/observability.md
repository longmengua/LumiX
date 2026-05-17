<!-- 檔案用途：記錄 tracing、audit log 與事件傳遞的可觀測性基線。 -->
# Observability Baseline

這份文件說明目前 tracing 與 audit 的基線能力。它還不是完整 production observability stack；metrics、dashboard 與 alert 仍追蹤在 [todo.md](todo.md)。

## Trace Headers

HTTP request 入口使用：

- `X-Request-Id`：單次 request identifier。未提供時 API 會產生 UUID。
- `X-Correlation-Id`：跨服務 correlation identifier。未提供時預設等於 `X-Request-Id`。

`RequestLoggingInterceptor` 會把兩個值寫入 response headers，並放進 MDC 的 `requestId`、`correlationId` 與 `traceId`。`GlobalExceptionHandler` 會在錯誤回應中帶回 `traceId`。

## 傳遞範圍

- OkHttp 對外呼叫會把目前 MDC trace headers 放進 `X-Request-Id` 與 `X-Correlation-Id`，除非 outgoing request 已自行設定。
- Kafka domain event 發布會把 trace headers 存進 outbox event，並寫成 Kafka record headers。
- Outbox relay 會重用儲存的 headers，因此延遲 retry 仍保留原始 request/correlation context。

## Audit Logs

Protected API security 與 authentication audit logs 在 request 通過 request logging interceptor 時會帶 `requestId`。Cancel-on-disconnect 核心事件會記錄 `uid`、`symbol` 與取消訂單數。

## Operations Metrics

`GET /api/ops/metrics` 會回傳 in-process snapshot，包含訂單狀態計數、下單延遲平均/最大值、撤單數與送出的成交事件數。這是本機營運基線；production 仍應匯出到專用 metrics backend。
