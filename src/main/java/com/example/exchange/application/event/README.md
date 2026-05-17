# application/event

事件發布抽象層。

目前狀態：
- `DomainEventPublisher` 是 application/domain 對 Kafka/outbox 的抽象入口。
- 具體發布由 `infra/kafka` 實作。

注意：
- application 不應直接依賴 KafkaTemplate。
- tracing headers / outbox 行為應由 publisher adapter 統一處理。
