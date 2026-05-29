# domain/service

Domain services 與 Polymarket domain workflow。

目前重點：
- 內部交易所：`MatchingEngine` contract、`OrderBook`、`OrderBookSnapshot`。
- 做市商：`HedgeVenueAdapter` 對沖 venue 送單 contract、`HedgeVenueFillMapper` 將 external venue fill message 轉成 durable hedge fill audit record。
- Polymarket：market discovery/sync、price refresh、approval、session signer、CLOB auth/signing/trading、local/CLOB order state-machine guard、user WebSocket。

目前狀態：
- Matching core 仍是 in-memory MVP，具 per-symbol sequencer baseline。
- Polymarket signing flow 不依賴外部 SDK，主要自行處理 EIP-712 / HMAC。

注意：
- 這層應描述業務能力，不直接決定 Redis key 或 Kafka topic。
