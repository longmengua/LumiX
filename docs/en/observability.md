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

`GET /api/ops/metrics` returns an in-process snapshot for order status counts, order latency average/max, canceled order count, emitted trade-event count, matching latency average/max, matching rejection rate, matching fill rate, DB operation latency average/max, and Redis operation latency average/max. This is a lightweight baseline for local operation; production should still export metrics through a dedicated metrics backend.
