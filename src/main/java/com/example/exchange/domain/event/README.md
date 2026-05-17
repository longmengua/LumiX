# domain/event

Domain events，描述已發生的業務事實。

目前狀態：
- `TradeExecuted`：撮合成交事件，可帶 seq 用於 replay。
- `OrderLifecycleEvent`：訂單 created/accepted/updated/rejected/canceled/expired/filled。
- `FundingSettled`、`PositionLiquidated`、`SnapshotCreated` 等風控與恢復事件。

注意：
- event 應偏 immutable，不直接持有 infrastructure 物件。
- 新事件要同步考慮 Kafka topic、outbox、consumer、文件與 schema version。
