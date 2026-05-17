# domain/service

Domain services 與 Polymarket domain workflow。

目前重點：
- 內部交易所：`MatchingEngine` contract、`OrderBook`、`OrderBookSnapshot`。
- Polymarket：market discovery/sync、price refresh、approval、session signer、CLOB auth/signing/trading、user WebSocket。

目前狀態：
- Matching core 仍是 in-memory MVP，具 per-symbol sequencer baseline。
- Polymarket signing flow 不依賴外部 SDK，主要自行處理 EIP-712 / HMAC。

注意：
- 這層應描述業務能力，不直接決定 Redis key 或 Kafka topic。
