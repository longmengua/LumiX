# infra

Infrastructure adapters and platform integrations.

- `metrics/OperationalMetricsMeterBinder` exports the in-process operational metrics snapshot to Micrometer / Prometheus.

Infrastructure adapters。

目錄：
- `config/`：Spring configuration properties、HTTP/Kafka/Redis/Web3j config。
- `kafka/`：domain event publisher 與 event store adapter。
- `redis/`：Redis repository implementations。
- `matching/`：in-memory matching engine adapter。
- `hedging/`：hedge venue adapters，預設安全拒絕送單，並提供 idempotency / retry / throttle decorators。
- `tracing/`：request/correlation id helper。

目前狀態：
- 多數 adapter 是 MVP / local dev baseline。
- production 前仍要補更完整 durable storage、distributed tracing exporter、alert backend 與 transaction boundary 營運強化。
