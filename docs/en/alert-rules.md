<!-- File purpose: Production alert-rule baseline for matching, Kafka/outbox, reconciliation, and asset balance signals. Chinese version: ../zh-TW/alert-rules.md. -->
# Alert Rules Baseline

These rules are the production alert contract for the current MVP signals. They are written so an alert backend can map them to Prometheus, Grafana, PagerDuty, Slack, or OpsGenie without changing application semantics.

## Rules

| Alert | Signal | Threshold | Severity | Route | First Runbook |
| --- | --- | --- | --- | --- | --- |
| Matching halt | No active matching owner for a traded symbol, duplicate owners for the same symbol, or checkpoint lag increasing for 3 consecutive checks | Fire immediately for duplicate owners; fire after 2 missed lease windows for no owner; fire after 5 minutes of growing lag | Critical | `ops.matching` | [Matching Sequencer Runbook](matching-sequencer-runbook.md) |
| Kafka lag | `kafkaLagMax` or backend consumer-group lag for order, trade, market-data, outbox, or archive consumers | Warning at `> 1,000` messages for 10 minutes; critical at `> 10,000` messages or monotonic growth for 30 minutes | Warning/Critical | `ops.kafka` | [Kafka Topics](kafka-topics.md) |
| DLQ buildup | Durable DLQ row count, Redis legacy `dlq:index` growth, repeated `DEAD` outbox event type, or oldest pending outbox age | Warning when DLQ count grows for 15 minutes; critical when oldest pending age exceeds 30 minutes or the same event type repeatedly reaches `DEAD` | Warning/Critical | `ops.outbox` | [Outbox Runbook](outbox-runbook.md) |
| Reconciliation failure | Reconciliation report contains `ERROR` issues or scheduler fails to produce a report within the expected window | Critical on any `ERROR`; warning when a scheduled run is missing by 2 intervals | Critical | `ops.reconciliation` | [Finance Operator Runbook](finance-operator-runbook.md) |
| External API error rate | CLOB/Gamma/hedge venue/Web3 RPC command failures, unresolved outcomes, or retry exhaustion | Warning when 5-minute error rate exceeds 5%; critical above 20% or when unresolved command outcomes persist beyond SLA | Warning/Critical | `ops.external-api` | [Observability Baseline](observability.md) |
| Unbalanced assets | Trial balance debit/credit mismatch, daily finance report imbalance, ledger replay mismatch, or restore smoke/replay validation failure | Critical on any non-zero imbalance or replay mismatch | Critical | `ops.finance` | [Finance Operator Runbook](finance-operator-runbook.md) |

## Routing Rules

- Critical matching, reconciliation, and unbalanced-asset alerts page the on-call operator.
- Warning Kafka, DLQ, and external API alerts create operations tickets and escalate to paging if they remain open past the critical window.
- Every alert payload should include `requestId` or `correlationId` when available, plus symbol, topic, partition, outbox id, report id, command id, or account id as applicable.
- Acknowledging an alert must not mutate trading state; recovery actions must use the relevant runbook/API.
- `AlertDispatchService` is the application dispatch boundary; `alerts.backend.*` controls the disabled-by-default webhook transport.

## Noise Controls

- Health-check and metrics endpoints must not trigger external API or tracing volume alerts.
- Maintenance windows can mute Kafka lag and archive/export alerts only when matching, order entry, and settlement flows are explicitly halted.
- Repeated alerts for the same symbol/topic/report should group by route plus entity id.
