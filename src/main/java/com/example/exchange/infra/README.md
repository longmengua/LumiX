# infra

Infrastructure adapters。

目錄：
- `config/`：Spring configuration properties、HTTP/Kafka/Redis/Web3j config。
- `kafka/`：domain event publisher 與 event store adapter。
- `redis/`：Redis repository implementations。
- `matching/`：in-memory matching engine adapter。
- `tracing/`：request/correlation id helper。

目前狀態：
- 多數 adapter 是 MVP / local dev baseline。
- production 前要補 durable storage、metrics、distributed tracing、transaction boundary。
