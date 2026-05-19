<!-- 檔案用途：durable outbox、DLQ replay 與人工補償的營運 runbook；英文版位於 ../en/outbox-runbook.md。 -->
# Outbox Runbook

English version: [../en/outbox-runbook.md](../en/outbox-runbook.md)

## 範圍

Production outbox 現在透過 JPA repository 使用 MySQL 的 `outbox_events` 與 `dlq_events`。Redis outbox keys 保留為 legacy hot-state adapter，不應作為 production source of truth。

## 檢查 DLQ

```bash
./shells/api-curls/exchange/recovery-outbox-dlq-get.sh
```

若有 DLQ item，先看 `topic`、`eventKey`、`eventType`、`payload`、`attempts`、`error`，並用 `outboxId` 對回 `outbox_events.id`。

## Replay

只有在 downstream 故障已修復，且事件語意仍有效時才 replay。

```bash
curl -sS -X POST "http://localhost:8080/api/recovery/outbox/dead/{outboxId}/replay"
```

預期狀態變更：

- `outbox_events.status`：`DEAD` -> `PENDING`
- `attempts`：重設為 `0`
- `last_error`：清空
- `next_attempt_at`：設為現在

relay job 下一輪會重新發布。

## 人工補償

當 replay 會造成已手動修復的副作用重複，或 payload 語意已不再有效時，使用 compensation。

```bash
curl -sS -X POST "http://localhost:8080/api/recovery/outbox/dead/{outboxId}/compensate?reason=rebuilt-projection"
```

標記 compensated 前：

- 在 incident ticket 記錄外部或人工處理動作。
- 確認目標 projection、account 或 downstream consumer state 已正確。
- reason 要寫具體原因，不要只寫 `manual`。

預期狀態變更：

- `outbox_events.status`：`DEAD` -> `COMPENSATED`
- `last_error`：以 `COMPENSATED:` 開頭
- `next_attempt_at`：清空

## 升級處理

若 DLQ 持續成長、最舊 pending outbox age 持續增加，或同一種 `eventType` 重複進入 `DEAD`，應升級給營運處理。這些訊號也應納入 Kafka lag、DLQ 堆積與外部 API 錯誤率的 alerting backlog。
