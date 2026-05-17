# com.example.exchange

交易所應用的 Java package root。

主要分層：
- `interfaces/`：REST、WebSocket/SSE、DTO、security/interceptor、Kafka consumers。
- `application/`：use cases、application services、commands、schedulers。
- `domain/`：entities、events、repository contracts、domain services、utility。
- `infra/`：Redis/Kafka/HTTP/Web3j/matching/config adapters。

目前狀態：
- MVP 支援內部交易所下單、撮合、帳務、風控、market data、recovery。
- Polymarket integration 支援市場同步、session signer、CLOB signing/order flow、user WebSocket。
- production TODO 以 `docs/*/todo.md` 為準。
