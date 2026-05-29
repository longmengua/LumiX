<!-- 檔案用途：外部 API retry / idempotency inventory；英文版位於 ../en/external-api-idempotency.md。 -->
# External API Idempotency

## Inventory

| Integration | Current client | Operation type | Retry/circuit/rate limit | Idempotency status |
| --- | --- | --- | --- | --- |
| Gamma market API | `PredictionGammaMarketClient` | Read-only market discovery | 共用 OkHttp timeout/retry/circuit/rate-limit baseline | Read-only，可重試；response schema versioning 仍待補。 |
| Polymarket CLOB place/cancel | `PolymarketClobTradingClient` via `PolymarketOrderService` | 外部 effectful writes | 使用 HTTP client 的路徑會套共用 OkHttp baseline | 仍待補：local/CLOB state machine 與 idempotent place/cancel/reconcile commands。 |
| Polymarket relayer/RPC approval | `PolymarketSessionService` / Web3j config | 外部 effectful writes and reads | 部分共用 config；RPC-specific limits 仍待補 | 仍待補：approval cache、expiry 與 idempotent transaction tracking。 |
| Hedge venue submit | `HedgeVenueAdapter` | 外部 effectful write | `RetryingHedgeVenueAdapter`、`ThrottlingHedgeVenueAdapter` | Baseline：`IdempotentHedgeVenueAdapter` 要求 `refId`，回傳 cached terminal results，拒絕 refId conflict，並在 timeout-like uncertain outcome 後阻止 duplicate submit。 |
| Bank/chain callbacks | Future callback clients/controllers | 外部 effectful reconciliation | 尚未實作 | 仍待補。 |

## Hedge Submit Baseline

Hedge venue submit 使用 `HedgeOrderRequest.refId` 作為外部 idempotency key。Idempotency decorator 會保存每個 `refId` 第一次 payload 的 fingerprint。

- 相同 `refId` 且相同 payload，在 accepted 或 non-retryable result 後重送時直接回傳已保存結果，不再呼叫 venue。
- 相同 `refId` 但 payload 不同時，以 `HEDGE_VENUE_IDEMPOTENCY_CONFLICT` 拒絕。
- 相同 `refId` 在 retryable / timeout-like result 後重送時，以 `HEDGE_VENUE_OUTCOME_UNCERTAIN` 阻止第二次外部 effect，因為 venue 可能已收到第一次 request。
- 缺少 `refId` 時，在呼叫 venue 前拒絕。

這是目前 MVP 的 in-process baseline。Production venue adapter 仍需要 durable idempotency storage、venue order lookup/reconciliation、uncertain outcome operator handling，以及 integration-specific rate limits。
