<!-- File purpose: Observability baseline for tracing, audit logs, and event propagation. -->
# Observability Baseline

This document covers the current tracing and audit baseline. It is not a full production observability stack yet; metrics, dashboards, and alerts are still tracked in [todo.md](todo.md).

## Trace Headers

Inbound HTTP requests use:

- `X-Request-Id`: per-request identifier. If omitted, the API generates a UUID.
- `X-Correlation-Id`: cross-service correlation identifier. If omitted, it defaults to `X-Request-Id`.

`RequestLoggingInterceptor` writes both values to the response headers and stores them in MDC as `requestId`, `correlationId`, and `traceId`. `GlobalExceptionHandler` returns `traceId` in error responses.

## Propagation

- OkHttp outbound calls copy the current MDC trace headers into `X-Request-Id` and `X-Correlation-Id` when the outgoing request does not already define them.
- Kafka domain event publishing stores trace headers on the outbox event and writes them as Kafka record headers.
- Outbox relay reuses the stored headers, so delayed retries keep the original request/correlation context.

## Audit And Core Event Logs

Protected API security and authentication audit logs include `requestId` when the request passed through the request logging interceptor. Core cancel-on-disconnect events are logged with `uid`, `symbol`, and canceled order count.

Order lifecycle projection emits structured `CORE_EVENT eventType=ORDER_LIFECYCLE` log lines with stable `uid`, `orderId`, `clientOrderId`, `symbol`, `stage`, `status`, `reasonCode`, and `eventTs` fields so operators can search core order state transitions without parsing free-form text.

## Operations Metrics

`GET /api/ops/metrics` returns an in-process JSON snapshot for order status counts, order latency average/max, canceled order count, emitted trade-event count, matching latency average/max, matching rejection rate, matching fill rate, DB operation latency average/max, Redis operation latency average/max, and Kafka consumer lag total/max.

Production metrics export is available through Spring Boot Actuator and Micrometer Prometheus:

- Scrape endpoint: `GET /actuator/prometheus`.
- Exposure config: `management.endpoints.web.exposure.include=health,info,prometheus`.
- Export toggle: `MANAGEMENT_PROMETHEUS_EXPORT_ENABLED`.
- `OperationalMetricsMeterBinder` maps the in-process snapshot into `exchange.*` Prometheus meters.

Protect `/actuator/prometheus` at the deployment edge or management network; the app exposes the endpoint so Prometheus can scrape it.

## Tracing Export And Sampling

`tracing.export.*` defines the disabled-by-default OpenTelemetry/OTLP export contract:

- `enabled`: remains `false` until the collector endpoint, retention, and data classification review are ready.
- `otlp-endpoint`: target collector endpoint, supplied from environment/secret-backed config in production.
- `service-name`: stable service identifier for trace backend search.
- `sample-rate`: default ratio sampling for ordinary requests; production default is `0.10`.
- `always-sample-critical-flows`: keep error, security audit, settlement, reconciliation, liquidation, and external command traces even when ratio sampling would drop them.
- `drop-health-and-metrics`: skip `/actuator/health`, readiness, and metrics reads to avoid low-value trace volume.

Dashboards should group by service name, route, status, external venue, Kafka topic, and matching symbol.

Exporter wiring is present through `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, and Spring Boot Actuator management properties:

- `management.tracing.enabled=${TRACING_EXPORT_ENABLED:false}`
- `management.tracing.sampling.probability=${TRACING_EXPORT_SAMPLE_RATE:0.10}`
- `management.otlp.tracing.endpoint=${TRACING_EXPORT_OTLP_ENDPOINT:http://localhost:4318/v1/traces}`

The first dashboard contract is documented in [Tracing Dashboard](tracing-dashboard.md).

## Alert Rules

The production alert baseline is documented in [Alert Rules Baseline](alert-rules.md). It covers matching halt, Kafka lag, DLQ buildup, reconciliation failure, external API error rate, and unbalanced assets, with routes and first runbooks for each alert.
