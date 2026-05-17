# interfaces/consumer

Kafka consumer 入口。

目前內容：
- `TradeEventConsumer`：消費成交事件並推送 market/user events。
- `PolymarketUserEventConsumer`：處理 Polymarket user-channel events。

目前狀態：
- MVP 以事件轉推與本地狀態更新為主。
- production 仍需 consumer checkpoint、dedup、DLQ、lag monitoring。
