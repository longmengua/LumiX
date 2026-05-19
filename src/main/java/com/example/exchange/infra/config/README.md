# infra/config

Spring Boot configuration 與 properties。

目前重點：
- `SecurityControlsProperties` / `ApiAuthProperties`：API security baseline。
- `RiskControlsProperties` / `RiskSnapshotProperties` / `FundingRateProperties` / `MarkPriceOracleProperties`：風控開關、risk snapshot 排程、funding config 與 mark/index price baseline。
- `OkHttpConfig`：外部 API timeout/retry/circuit breaker/rate limit baseline。
- `KafkaConfig`、`RedisConfig`、`Web3jConfig`、`PolymarketConfigs`。
- `DefaultSymbolConfigRepository`：MVP symbol config source。

注意：
- secrets 不應硬編碼；正式環境走 env/secret manager。
- 新 config 要同步 `application.yml`、`application-dev.yml`、`application-prod.yml`。
