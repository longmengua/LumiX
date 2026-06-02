# infra/tracing

Tracing helper。

目前內容：
- `TraceContext`：處理 request id / correlation id 的 MDC、header 傳遞與 log-safe normalize。

目前狀態：
- 已有 header/MDC/outbox/Kafka/external API 傳遞 baseline。
- 已有 `tracing.export.*` disabled-by-default export/sampling policy config。
- production 仍需接實際 OpenTelemetry/OTLP exporter dependency 與 dashboard。

注意：
- request 結束要清 MDC，避免 servlet thread reuse 污染下一個請求。
- tracing id 要 sanitize，避免 log injection。
