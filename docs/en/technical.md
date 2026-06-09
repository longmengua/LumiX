<!-- File purpose: English technical documentation index. Chinese version: ../zh-TW/technical.md. -->
# Technical Documentation

Technical documentation is for engineers who need to understand how the system is structured, operated, and extended.

中文版本：[../zh-TW/technical.md](../zh-TW/technical.md)

## Documents

| Document | Description |
| --- | --- |
| [Current State](current-state.md) | Quick status dashboard for MVP baseline, completed capabilities, production blockers, and near-term priorities. |
| [Technical Notes](NOTES.md) | Architecture and implementation review covering layers, strengths, risks, and production priorities. |
| [AI Code Map](../ai/code-map.md) | Compact code ownership and flow map for coding agents. |
| [AI Documentation](ai.md) | English index for agent maps and task-entry workflow. |
| [Task Documentation](tasks.md) | English index for selectable task files. |
| [API curl scripts](../../shells/api-curls/README.md) | Local curl-script guide for exchange and Polymarket API endpoints. |
| [Matching Engine](../../src/main/java/com/example/exchange/infra/matching/README.md) | Notes for the current in-memory matching engine and its production evolution path. |
| [Redis Key Schema](redis-key-schema.md) | Redis key ownership, TTL rules, namespace policy, and migration backlog. |
| [Kafka Topics](kafka-topics.md) | Topic matrix, partition key policy, consumer groups, schema versioning, and production creation rules. |
| [Observability Baseline](observability.md) | Request/correlation ID propagation across HTTP, OkHttp, Kafka outbox, and audit logs. |
| [Archive Strategy](archive-strategy.md) | Archive manifests, retention classes, delete preconditions, and restore rules for historical data. |
| [Finance Operator Runbook](finance-operator-runbook.md) | Daily finance export, ledger archive restore smoke, delete guard, replay validation, and unbalanced report handling. |
| [Outbox Runbook](outbox-runbook.md) | Durable outbox, DLQ replay, and manual compensation operations. |
| [Alert Rules Baseline](alert-rules.md) | Matching halt, Kafka lag, DLQ buildup, reconciliation failure, external API error rate, and unbalanced-asset alert rules. |
| [Tracing Dashboard](tracing-dashboard.md) | OTLP tracing exporter wiring, dashboard panels, and sampling controls. |
| [Cross-Store Failure Drill](cross-store-failure-drill.md) | MySQL/Redis/Kafka failure drill and outbox/domain-state consistency checks. |
| [Disaster Recovery Runbook](disaster-recovery-runbook.md) | Matching/order/account/position restore order, worker takeover, authenticated reconnect replay, and smoke commands. |
| [ADL Operator Runbook](adl-operator-runbook.md) | ADL queue claim, execution, partial retry, no-candidate retry, and insurance reconciliation operations. |
| [Matching Sequencer Runbook](matching-sequencer-runbook.md) | Production ownership, partitioning, startup, and failover rules for per-symbol sequencers. |
| [Product overview](README.md) | Business features, modules, order placement flow, APIs, and Kafka topics. |
| [Production TODO](todo.md) | Engineering roadmap for production readiness. |

## Scope

- System architecture and package boundaries.
- Local infrastructure: MySQL, Redis, Kafka, Kafka UI.
- API test scripts and manual verification flows.
- Matching engine behavior and current limitations.
- Current completion level, MVP baseline, and production blockers.
- Production engineering gaps that need implementation before real traffic.
