# domain/repository/client

外部 API client contracts。

目前狀態：
- `PredictionGammaMarketClient` 負責 Gamma market discovery / fetch。

注意：
- 這層是 domain-facing contract，不應放 OkHttp config。
- Timeout、retry、circuit breaker、rate limit 應由 infra config / adapter 統一處理。
