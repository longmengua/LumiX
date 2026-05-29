<!-- 檔案用途：外部 API retry / idempotency inventory；英文版位於 ../en/external-api-idempotency.md。 -->
# External API Idempotency

## Inventory

| Integration | Current client | Operation type | Retry/circuit/rate limit | Idempotency status |
| --- | --- | --- | --- | --- |
| Gamma market API | `PredictionGammaMarketClient` | Read-only market discovery | 共用 OkHttp timeout/retry/circuit/rate-limit baseline | Read-only，可重試；response schema versioning 仍待補。 |
| Polymarket CLOB place/cancel | `PolymarketClobTradingClient` via `PolymarketOrderService` / `PolymarketOrderTrackingService` | 外部 effectful writes | 使用 HTTP client 的路徑會套共用 OkHttp baseline | Place baseline：可選 `clientRequestId` 會成為 local idempotency key。Cancel baseline：已記錄 cancel 狀態時直接回 local order，不再重送 DELETE。Sync/reconcile idempotency 仍待補。 |
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

## CLOB Place Baseline

Polymarket place order 支援可選 `clientRequestId`。有帶時，`PolymarketOrderService` 會把它作為 `internalOrderId`，並在消耗 session limit、檢查 approval、簽名或呼叫 CLOB `/order` 之前先查 local order table。

- 相同 `clientRequestId` 且 payload 相同時，直接回傳既有 local/CLOB result，不再呼叫 CLOB。
- 相同 `clientRequestId` 但 user/session/market/direction/amount/type 不同時，回傳 `IDEMPOTENCY_CONFLICT`。
- 既有 local order 尚無 terminal CLOB result 時，回傳 `CLOB_OUTCOME_UNCERTAIN`，不再重送 `/order`。
- 未提供 `clientRequestId` 的 request 保持原本自動產生 internal id 的行為。

## CLOB Cancel Baseline

Polymarket cancel 使用 local order status 作為 retry boundary。若前一次 cancel 已記錄 `CANCEL_REQUESTED`、`CANCELED`、`CANCELLED` 或 `ORDER_STATUS_CANCELED`，`PolymarketOrderTrackingService.cancelOrder` 會直接回傳 local order，不再呼叫第二次 CLOB DELETE。

- 第一次 CLOB cancel 成功後，會保存 raw CLOB payload、`lastSyncedAt` 與 `CANCEL_REQUESTED`。
- local cancel marker 已存在時，duplicate cancel request 不會再送外部 command。
- 這仍是 local baseline；更完整的 CLOB state-machine transitions，以及 uncertain cancel outcome 的 remote lookup/reconcile 還待補。
