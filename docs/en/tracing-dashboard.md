<!-- File purpose: production tracing exporter and dashboard contract. Chinese version: ../zh-TW/tracing-dashboard.md. -->
# Tracing Dashboard

This document defines the production tracing export and first dashboard baseline.

## Export Wiring

The application includes:

- `micrometer-tracing-bridge-otel`
- `opentelemetry-exporter-otlp`
- Spring Boot Actuator tracing auto-configuration

Runtime controls:

| Setting | Default | Purpose |
| --- | --- | --- |
| `TRACING_EXPORT_ENABLED` | `false` | Enables `management.tracing.enabled`. |
| `TRACING_EXPORT_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP HTTP collector endpoint. |
| `TRACING_EXPORT_SAMPLE_RATE` | `0.10` | Maps to `management.tracing.sampling.probability`. |
| `TRACING_EXPORT_SERVICE_NAME` | `java21-match-hub` | Stable service label for dashboard grouping. |

Keep export disabled until a collector, retention policy, and access controls are ready.

## Dashboard Baseline

Create a Grafana / Tempo dashboard with these panels:

| Panel | Query / Grouping | Why |
| --- | --- | --- |
| HTTP route latency | service name, route, method, status | Find slow or failing API routes. |
| Protected API failures | route, status, `requestId` / `correlationId` logs | Correlate auth/rate-limit failures with logs. |
| External venue calls | service name, remote host, status | Isolate Polymarket / hedge venue latency and failures. |
| Kafka/outbox publish flow | topic, event type, correlation id | Follow delayed outbox publish and consumer retry paths. |
| Critical workflows | liquidation, ADL, reconciliation, hedge execution | Keep incident triage focused on money-moving flows. |

Recommended dashboard links:

- Link traces to logs by `traceId`, `requestId`, and `correlationId`.
- Link route panels to `/actuator/prometheus` metrics for rate/error/latency context.
- Link reconciliation and alert panels to the runbooks in [Alert Rules Baseline](alert-rules.md).

## Sampling Policy

Use `TRACING_EXPORT_SAMPLE_RATE` for ordinary traffic. Keep low-value health and metrics reads out of trace review using the `tracing.export.drop-health-and-metrics` policy. Critical flows such as security audit, settlement, reconciliation, liquidation, ADL, and external command execution should remain always-sampled once route-level sampling hooks are added.
