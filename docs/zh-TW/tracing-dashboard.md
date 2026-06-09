<!-- 檔案用途：production tracing exporter 與 dashboard contract。英文版位於 ../en/tracing-dashboard.md。 -->
# Tracing Dashboard

本文件定義 production tracing export 與第一版 dashboard baseline。

## Export Wiring

Application 已包含：

- `micrometer-tracing-bridge-otel`
- `opentelemetry-exporter-otlp`
- Spring Boot Actuator tracing auto-configuration

Runtime controls：

| Setting | Default | 用途 |
| --- | --- | --- |
| `TRACING_EXPORT_ENABLED` | `false` | 啟用 `management.tracing.enabled`。 |
| `TRACING_EXPORT_OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP HTTP collector endpoint。 |
| `TRACING_EXPORT_SAMPLE_RATE` | `0.10` | 對應 `management.tracing.sampling.probability`。 |
| `TRACING_EXPORT_SERVICE_NAME` | `java21-match-hub` | Dashboard grouping 使用的穩定 service label。 |

在 collector、retention policy 與 access controls 都準備好以前，export 維持關閉。

## Dashboard Baseline

建立 Grafana / Tempo dashboard，至少包含：

| Panel | Query / Grouping | 用途 |
| --- | --- | --- |
| HTTP route latency | service name、route、method、status | 找出慢或失敗的 API routes。 |
| Protected API failures | route、status、`requestId` / `correlationId` logs | 串 auth / rate-limit failure 與 logs。 |
| External venue calls | service name、remote host、status | 隔離 Polymarket / hedge venue latency 與 failures。 |
| Kafka/outbox publish flow | topic、event type、correlation id | 追 outbox delayed publish 與 consumer retry path。 |
| Critical workflows | liquidation、ADL、reconciliation、hedge execution | 讓 incident triage 聚焦 money-moving flows。 |

建議 dashboard links：

- 用 `traceId`、`requestId`、`correlationId` 連到 logs。
- Route panels 連到 `/actuator/prometheus` metrics，補 rate/error/latency context。
- Reconciliation 與 alert panels 連到 [Alert Rules Baseline](alert-rules.md) 的 runbooks。

## Sampling Policy

一般流量使用 `TRACING_EXPORT_SAMPLE_RATE`。低價值 health / metrics reads 依 `tracing.export.drop-health-and-metrics` policy 不納入 trace review。Security audit、settlement、reconciliation、liquidation、ADL、external command execution 等 critical flows，後續加上 route-level sampling hooks 後應固定 always-sample。
