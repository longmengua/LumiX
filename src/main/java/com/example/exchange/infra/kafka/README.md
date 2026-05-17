# infra/kafka

Kafka infrastructure adapters。

目前內容：
- `KafkaDomainEventPublisher`：發布 trade、order lifecycle、funding、liquidation 等 domain events。
- `KafkaEventStore`：MVP trade event store / replay index。
- `KafkaEventRoute`：topic / key routing helper。

目前狀態：
- outbox baseline 已存在，但 production durable outbox 仍待補。
- topic 設計文件在 `docs/*/kafka-topics.md`。

注意：
- partition key 要穩定；高價值事件不要使用 random key。
- consumer offset 不等於 business checkpoint。
