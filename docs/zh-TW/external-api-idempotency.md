<!-- 檔案用途：外部 API retry / idempotency inventory；英文版位於 ../en/external-api-idempotency.md。 -->
# External API Idempotency

## Inventory

| Integration | Current client | Operation type | Retry/circuit/rate limit | Idempotency status |
| --- | --- | --- | --- | --- |
| Gamma market API | `PredictionGammaMarketClient` | Read-only market discovery | 共用 OkHttp timeout/retry/circuit/rate-limit baseline | Read-only，可重試；response schema versioning 仍待補。 |
| Polymarket CLOB place/cancel/sync/reconcile | `PolymarketClobTradingClient` via `PolymarketOrderService` / `PolymarketOrderTrackingService` | 外部 effectful writes plus read-only status sync | 使用 HTTP client 的路徑會套共用 OkHttp baseline | Place baseline：可選 `clientRequestId` 會成為 local idempotency key。Cancel baseline：已記錄 cancel/uncertain 狀態時直接回 local order，不再重送 DELETE。Sync/reconcile baseline：未變更的 CLOB payload 不再重複寫 local row。 |
| Polymarket relayer/RPC approval | `PolymarketSessionService` / `PolymarketApprovalService` / Web3j config | 外部 effectful writes and reads | 部分共用 config；RPC-specific limits 仍待補 | Approval reads 已有 TTL cache 與 owner-scoped clear。仍待補：backend-observed effectful flow 的 idempotent approval transaction tracking。 |
| Hedge venue submit | `HedgeVenueAdapter` | 外部 effectful write | `RetryingHedgeVenueAdapter`、`ThrottlingHedgeVenueAdapter` | Baseline：`IdempotentHedgeVenueAdapter` 要求 `refId`，會先 claim 再送 venue、durably 保存 terminal results、拒絕 refId conflict，並在 pending/uncertain outcome 後阻止 duplicate submit。 |
| Bank/chain callbacks | Future callback clients/controllers | 外部 effectful reconciliation | 尚未實作 | 仍待補。 |

## Hedge Submit Baseline

Hedge venue submit 使用 `HedgeOrderRequest.refId` 作為外部 idempotency key。Idempotency decorator 會透過 `HedgeVenueIdempotencyStore` 保存每個 `refId` 第一次 payload 的 fingerprint；Spring wiring 使用 JPA-backed store。

- 相同 `refId` 且相同 payload，在 accepted 或 non-retryable result 後重送時直接回傳已保存結果，不再呼叫 venue。
- 相同 `refId` 但 payload 不同時，以 `HEDGE_VENUE_IDEMPOTENCY_CONFLICT` 拒絕。
- 相同 `refId` 在 first claim 仍 pending，或 retryable / timeout-like result 後重送時，以 `HEDGE_VENUE_OUTCOME_UNCERTAIN` 阻止第二次外部 effect，因為 venue 可能已收到第一次 request。
- 缺少 `refId` 時，在呼叫 venue 前拒絕。

Durable baseline 仍需要 venue order lookup/reconciliation、uncertain outcome operator handling，以及 integration-specific rate limits，才能接真實 venue adapter。

## CLOB Place Baseline

Polymarket place order 支援可選 `clientRequestId`。有帶時，`PolymarketOrderService` 會把它作為 `internalOrderId`，並在消耗 session limit、檢查 approval、簽名或呼叫 CLOB `/order` 之前先查 local order table。

- 相同 `clientRequestId` 且 payload 相同時，直接回傳既有 local/CLOB result，不再呼叫 CLOB。
- 相同 `clientRequestId` 但 user/session/market/direction/amount/type 不同時，回傳 `IDEMPOTENCY_CONFLICT`。
- 既有 local order 尚無 terminal CLOB result 時，回傳 `CLOB_OUTCOME_UNCERTAIN`，不再重送 `/order`。
- 未提供 `clientRequestId` 的 request 保持原本自動產生 internal id 的行為。

## CLOB Cancel Baseline

Polymarket cancel 使用可選 `commandId` 與 local order status 作為 retry boundary。有帶 `commandId` 時，`PolymarketOrderTrackingService` 會先在 `PolymarketClobCommandStore` claim，再送 CLOB DELETE，並在 local result 保存後 complete record。若前一次 cancel 已記錄 `CANCEL_REQUESTED`、`CANCEL_OUTCOME_UNCERTAIN`、`CANCELED`、`CANCELLED` 或 `ORDER_STATUS_CANCELED`，會直接回傳 local order，不再呼叫第二次 CLOB DELETE。

- 相同 `commandId` 且相同 cancel payload 時，直接回 local order，不再呼叫 CLOB。
- 相同 `commandId` 但 internal/CLOB order 不同時，在呼叫 CLOB 前拒絕。
- 第一次 CLOB cancel 成功後，會保存 raw CLOB payload、`lastSyncedAt` 與 `CANCEL_REQUESTED`。
- CLOB cancel 回 `EXCEPTION` 或 5xx outcome 時，會保存 raw payload、`lastSyncedAt`、`lastError` 與 `CANCEL_OUTCOME_UNCERTAIN`。
- local cancel marker 已存在時，duplicate cancel request 不會再送外部 command。
- Reconcile 會納入 `CANCEL_OUTCOME_UNCERTAIN` orders，並可用遠端 CLOB status 取代 local uncertain state。
- 這仍是 local baseline；更完整的 CLOB state-machine transitions 還待補。

## CLOB Sync/Reconcile Baseline

CLOB order sync 與 reconcile 對外是 read-only call，但遠端狀態變更時仍會更新 local order row。`PolymarketOrderTrackingService` 現在會先比較 incoming raw payload、status、matched size 與 error，再決定是否保存。

- 相同 CLOB payload 重複 sync 時，直接回 local order，不再 save database row。
- Reconcile 仍會把 order 視為 checked，但會另外回報 unchanged rows，且不寫入未變更的 row。
- 遠端 status、matched size、error 或 raw payload 有變化時，才保存並更新 `lastSyncedAt`。
- 更完整的 local/CLOB/trade/settlement state-machine transitions 仍待補。

## RPC Approval Read Baseline

`PolymarketApprovalService` 會依 contract、owner、spender/operator 快取 ERC20 allowance 與 ERC1155 approval reads，TTL 由 `polymarket.approval-cache-ttl-seconds` 控制。

- TTL 內重複查 approval 不會再送第二次 `eth_call`。
- `DELETE /api/prediction/approve/cache?owner=...` 可清除單一 owner 的 cache；不帶 owner 時清除全部 approval caches。
- Cache 過期後，order validation 使用前會重新從 RPC refresh。
- 未來若加入 backend-observed approval transaction / relayer effectful flow，仍需要 transaction idempotency tracking。
