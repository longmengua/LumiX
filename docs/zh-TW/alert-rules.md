<!-- 檔案用途：撮合、Kafka/outbox、對帳與資產平衡訊號的 production alert-rule baseline。英文版位於 ../en/alert-rules.md。 -->
# Alert Rules Baseline

這些規則是目前 MVP signals 的 production alert contract。Alert backend 可把它們映射到 Prometheus、Grafana、PagerDuty、Slack 或 OpsGenie，而不需要改 application semantics。

## 規則

| Alert | Signal | Threshold | Severity | Route | First Runbook |
| --- | --- | --- | --- | --- | --- |
| Matching halt | 交易中 symbol 沒有 active matching owner、同一 symbol 有多個 owner，或 checkpoint lag 連續 3 次檢查增加 | duplicate owners 立即告警；no owner 超過 2 個 lease window 告警；lag 持續增加 5 分鐘告警 | Critical | `ops.matching` | [Matching Sequencer Runbook](matching-sequencer-runbook.md) |
| Kafka lag | `kafkaLagMax` 或 backend consumer-group lag，涵蓋 order、trade、market-data、outbox、archive consumers | 10 分鐘 `> 1,000` messages 為 warning；`> 10,000` messages 或 30 分鐘單調增加為 critical | Warning/Critical | `ops.kafka` | [Kafka Topics](kafka-topics.md) |
| DLQ buildup | Durable DLQ row count、Redis legacy `dlq:index` growth、同一 outbox event type 重複進 `DEAD`，或 oldest pending outbox age | DLQ count 連續 15 分鐘增加為 warning；oldest pending age 超過 30 分鐘或同 event type 重複進 `DEAD` 為 critical | Warning/Critical | `ops.outbox` | [Outbox Runbook](outbox-runbook.md) |
| Reconciliation failure | Reconciliation report 含 `ERROR` issues，或 scheduler 未在預期 window 產生 report | 任一 `ERROR` 即 critical；scheduled run 超過 2 個 interval 未出現為 warning | Critical | `ops.reconciliation` | [Finance Operator Runbook](finance-operator-runbook.md) |
| External API error rate | CLOB/Gamma/hedge venue/Web3 RPC command failures、unresolved outcomes，或 retry exhaustion | 5 分鐘 error rate 超過 5% 為 warning；超過 20% 或 unresolved command outcomes 超過 SLA 為 critical | Warning/Critical | `ops.external-api` | [Observability Baseline](observability.md) |
| Unbalanced assets | Trial balance debit/credit mismatch、daily finance report imbalance、ledger replay mismatch，或 restore smoke/replay validation failure | 任一非零 imbalance 或 replay mismatch 即 critical | Critical | `ops.finance` | [Finance Operator Runbook](finance-operator-runbook.md) |

## Routing Rules

- Critical matching、reconciliation、unbalanced-asset alerts 需要 page on-call operator。
- Warning Kafka、DLQ、external API alerts 先建立 operations tickets；若超過 critical window 未解除再升級 paging。
- 每個 alert payload 應盡量帶 `requestId` 或 `correlationId`，並依情境帶 symbol、topic、partition、outbox id、report id、command id 或 account id。
- Acknowledge alert 不應改動交易狀態；recovery actions 必須走對應 runbook/API。

## Noise Controls

- Health-check 與 metrics endpoints 不應觸發 external API 或 tracing volume alerts。
- Maintenance window 只能在 matching、order entry、settlement flows 明確 halt 時 mute Kafka lag 與 archive/export alerts。
- 同一 symbol/topic/report 的重複 alert 應依 route 加 entity id group。
