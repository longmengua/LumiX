<!-- File purpose: Operational runbook for durable outbox, DLQ replay, and manual compensation. Chinese version: ../zh-TW/outbox-runbook.md. -->
# Outbox Runbook

中文版本：[../zh-TW/outbox-runbook.md](../zh-TW/outbox-runbook.md)

## Scope

The production outbox now uses MySQL tables `outbox_events` and `dlq_events` through the JPA repositories. Redis outbox keys are legacy hot-state adapters and should not be used as the production source of truth.

## Check DLQ

```bash
./shells/api-curls/exchange/recovery-outbox-dlq-get.sh
```

If a DLQ item exists, inspect `topic`, `eventKey`, `eventType`, `payload`, `attempts`, and `error`. Match the `outboxId` back to `outbox_events.id`.

## Replay

Replay only when the downstream failure is fixed and the event is still valid.

```bash
curl -sS -X POST "http://localhost:8080/api/recovery/outbox/dead/{outboxId}/replay"
```

Expected state change:

- `outbox_events.status`: `DEAD` -> `PENDING`
- `attempts`: reset to `0`
- `last_error`: cleared
- `next_attempt_at`: set to now

The relay job will publish it on the next run.

## Manual Compensation

Use compensation when replay would duplicate an already manually repaired side effect, or when the payload is no longer semantically valid.

```bash
curl -sS -X POST "http://localhost:8080/api/recovery/outbox/dead/{outboxId}/compensate?reason=rebuilt-projection"
```

Before marking compensated:

- Record the external/manual action in the incident ticket.
- Confirm the target projection, account, or downstream consumer state is correct.
- Include a concrete reason, not just `manual`.

Expected state change:

- `outbox_events.status`: `DEAD` -> `COMPENSATED`
- `last_error`: prefixed with `COMPENSATED:`
- `next_attempt_at`: cleared

## Escalation

Escalate to operations when DLQ grows, oldest pending outbox age keeps increasing, or the same `eventType` repeatedly reaches `DEAD`. These should feed the alerting backlog for Kafka lag, DLQ buildup, and external API error rate.
