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

## Audit And Core Event Logs

Protected API security 與 authentication audit logs 在 request 通過 request logging interceptor 時會帶 `requestId`。Cancel-on-disconnect 核心事件會記錄 `uid`、`symbol` 與取消訂單數。

Order lifecycle projection 會輸出 structured `CORE_EVENT eventType=ORDER_LIFECYCLE` log line，包含穩定的 `uid`、`orderId`、`clientOrderId`、`symbol`、`stage`、`status`、`reasonCode`、`eventTs` 欄位，讓營運能搜尋核心訂單狀態轉換而不用解析自由文字。

## Operations Metrics

`GET /api/ops/metrics` 會回傳 in-process snapshot，包含訂單狀態計數、下單延遲平均/最大值、撤單數、送出的成交事件數、matching latency 平均/最大值、matching rejection rate、matching fill rate、DB operation latency 平均/最大值、Redis operation latency 平均/最大值，以及 Kafka consumer lag total/max。這是本機營運基線；production 仍應匯出到專用 metrics backend。

## Tracing Export And Sampling

`tracing.export.*` 定義未來 OpenTelemetry/OTLP bridge 使用的 export contract，預設關閉：

- `enabled`：collector endpoint、retention 與資料分級 review 完成前維持 `false`。
- `otlp-endpoint`：target collector endpoint，production 由環境變數或 secret-backed config 提供。
- `service-name`：trace backend 搜尋用的穩定 service identifier。
- `sample-rate`：一般 request ratio sampling；production 預設 `0.10`。
- `always-sample-critical-flows`：error、security audit、settlement、reconciliation、liquidation、external command traces 即使 ratio sampling 會丟棄也要保留。
- `drop-health-and-metrics`：跳過 `/actuator/health`、readiness 與 metrics reads，避免低價值 trace volume。

Dashboard 應按 service name、route、status、external venue、Kafka topic 與 matching symbol 分組。實際 exporter wiring 仍是後續工作；目前 repo 提供 config 與 policy baseline。
