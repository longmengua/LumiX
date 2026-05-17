<!-- File purpose: English technical documentation index. Chinese version: ../zh-TW/technical.md. -->
# Technical Documentation

Technical documentation is for engineers who need to understand how the system is structured, operated, and extended.

中文版本：[../zh-TW/technical.md](../zh-TW/technical.md)

## Documents

| Document | Description |
| --- | --- |
| [Technical Notes](../../NOTES.md) | Architecture and implementation review covering layers, strengths, risks, and production priorities. |
| [API curl scripts](../../shells/api-curls/README.md) | Local curl-script guide for exchange and Polymarket API endpoints. |
| [Matching Engine](../../src/main/java/com/example/exchange/infra/matching/README.md) | Notes for the current in-memory matching engine and its production evolution path. |
| [Redis Key Schema](redis-key-schema.md) | Redis key ownership, TTL rules, namespace policy, and migration backlog. |
| [Kafka Topics](kafka-topics.md) | Topic matrix, partition key policy, consumer groups, schema versioning, and production creation rules. |
| [Observability Baseline](observability.md) | Request/correlation ID propagation across HTTP, OkHttp, Kafka outbox, and audit logs. |
| [Product overview](README.md) | Business features, modules, order placement flow, APIs, and Kafka topics. |
| [Production TODO](todo.md) | Engineering roadmap for production readiness. |

## Scope

- System architecture and package boundaries.
- Local infrastructure: MySQL, Redis, Kafka, Kafka UI.
- API test scripts and manual verification flows.
- Matching engine behavior and current limitations.
- Production engineering gaps that need implementation before real traffic.
