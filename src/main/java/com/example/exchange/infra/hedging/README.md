# infra/hedging

Hedge venue adapters。

目前內容：
- `RejectingHedgeVenueAdapter`：預設 adapter，在未設定外部 venue 前安全拒絕送單，避免 production 誤以為已接上真實對沖通道。
- `RealHedgeVenueAdapter` / `RealHedgeVenueOrderLookupAdapter` / `RealHedgeVenueHttpClient` / `RealHedgeVenueSigner` / `SignedHedgeVenueRequest`：真實 venue adapter baseline，固定 submit/lookup payload 與 HMAC header contract，透過注入的 `OkHttpClient` 執行 HTTP I/O，並映射 accepted/rejected/retryable venue response；未顯式啟用前安全拒絕實際送單或回傳空查詢。
- `NoopHedgeVenueOrderLookupAdapter`：預設 lookup adapter，在未設定外部 venue 前不回填 uncertain outcome，避免誤標完成。
- `IdempotentHedgeVenueAdapter`：Spring 預設 `@Primary` adapter，以 `refId` claim/result store 包住目前的 safe rejecting venue submit，阻止 duplicate submit、payload conflict、pending claim 與 timeout-like uncertain outcome 後的重送；接真實 venue 時需替換 delegate wiring。
- `RetryingHedgeVenueAdapter`：retry decorator baseline，依 `HedgeOrderResult.retryable` 對暫時性 venue 錯誤做有限次重試，並保留同一個 `refId` 作為冪等送單來源。
- `RetryBackoff` / `Sleeper`：抽象 retry delay 與 sleep 行為，讓 backoff 可測且可替換。
- `ThrottlingHedgeVenueAdapter`：throttle decorator baseline，限制連續送單最小間隔。

注意：
- 真實 venue adapter 接線時必須沿用 durable 冪等送單 store，並補齊 venue-specific response fields、憑證輪替、監控告警與 audit ref。
- 這些 decorator 不替代 production worker lock；接真實 venue 前要補外部限流、監控和告警。
