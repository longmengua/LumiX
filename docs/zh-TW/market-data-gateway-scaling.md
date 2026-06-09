<!-- 檔案用途：記錄 WebSocket/SSE market-data gateway 水平擴展部署注意事項。 -->
# Market Data Gateway Scaling

這份文件定義 WebSocket/SSE gateway 作為獨立 edge process 部署時的 production baseline。目前程式可讓 gateway 跟 Spring Boot app 一起跑；當行情 fanout 或長連線負載上升時，production 應把 gateway instances 從下單入口與 worker processes 拆開。

## Runtime Role

gateway instance 只啟用 HTTP、SSE、WebSocket、auth、stream rate limiting、heartbeat，以及 market-data read/replay 依賴。order command handling、matching、settlement 與 scheduled workers 應放在其他 role。

每個 gateway instance 只擁有本機連線 client。`PushGatewayService` 會把 subscriber sessions 保存在 memory，因此 gateway restart 或 node loss 只會中斷 pin 在該 node 的連線。Client 必須重新連線並使用 recovery cursor contract，不應依賴 server-side session migration。

明確設定 runtime role：

- `push-gateway.runtime.role=${PUSH_GATEWAY_RUNTIME_ROLE:GATEWAY}` 用於 gateway instances。
- `push-gateway.runtime.instance-id=${PUSH_GATEWAY_INSTANCE_ID:${HOSTNAME}}` 用於 logs/readiness。
- `push-gateway.runtime.accept-new-streams=${PUSH_GATEWAY_ACCEPT_NEW_STREAMS:true}` 可在維護時停止新 stream。
- `push-gateway.runtime.draining=${PUSH_GATEWAY_DRAINING:false}` 會讓新的 SSE/WebSocket stream 回 `503`，既有 stream 繼續 drain。

`GET /api/ops/push-gateway/status` 會回傳 instance id、role、accepting/draining flags、active SSE channel/subscriber counts，以及 active WebSocket channel/session counts。

## Fanout Topology

Public market-data events 使用 broadcast feed：

- Matching/trade workers 在 durable state changes commit 後，把 depth、trade、ticker、kline events 發到 Kafka 或其他 shared bus。
- 每個 gateway instance 都要獨立消費 broadcast feed，不要把所有 gateway 放在同一個 competing consumer group。每個 node 必須收到所有 public event，再依本機訂閱 channel 過濾。
- 如果使用 shared competing consumer group，單一 event 只會被其中一個 gateway node 收到，連在其他 node 的 clients 可能漏推送。

Private user streams 也遵守相同的 local-filtering rule，除非已建立 shared subscription registry 與 targeted routing layer。沒有該 registry 時，private events 應 broadcast 到所有 gateway instances，並在 node 上依 authenticated `uid` 過濾。

## Load Balancer

Load balancer 需要支援長連線 HTTP 與 WebSocket upgrade：

- 將 `/api/market-data/*/stream`、`/api/market-data/user/*/stream`、`/ws/market/*`、`/ws/user/*` route 到 gateway instances。
- gateway 拆分後，REST order-entry 與 admin APIs 應使用自己的 upstream pool。
- Sticky sessions 對一般 reconnect 行為是 optional，但可降低 churn。正確性不依賴 sticky sessions，因為 clients 會透過 durable cursors recovery。
- 部署或 scale-in 前要設定 connection draining。先停止接受新的 SSE/WebSocket handshakes，保留既有 streams 到 drain timeout，再用可重連狀態關閉。

## Shared Controls

In-process rate limiter 是安全的本機 baseline。Production multi-instance 部署應選一種策略：

- 使用 sticky sessions，並接受 per-node fixed-window limits 作為 soft control。
- 當 limit 必須是 per client 全域限制時，改用 Redis 或 gateway-edge shared counting 取代 local counter。

`api-auth.enabled=true` 時，private streams 必須保留 subscription authorization。Browser WebSocket clients 可以用 `apiKey`、`access_token` 或 `token` query parameters，但 TLS termination 與 access logs 必須把這些值視為 secrets。

## Heartbeat And Recovery

只在 gateway role 啟用 `push-gateway.heartbeat.enabled=true`。`push-gateway.heartbeat.fixed-delay-ms` 應低於 client idle timeout，也低於 load-balancer idle timeout。

Client 遇到 missed heartbeat 或 connection close 時，應用以下流程重連：

1. 呼叫 `GET /api/market-data/{symbol}/recovery-cursor`。
2. 用 `GET /api/market-data/{symbol}/depth-deltas?afterVersion=...` replay depth。
3. 用 `GET /api/market-data/{symbol}/trades?afterTs=...&afterMatchId=...` replay trades。
4. Replay catch up 後重新訂閱 SSE/WebSocket streams。

如果 replay window 已被 market-data retention 清掉，client 必須重載完整 depth snapshot，並從回傳的 version 繼續。

## Readiness And Rollback

Gateway instance ready 的條件是 auth config 已載入、`GET /api/ops/push-gateway/status` 回報 `acceptingNewStreams=true`、broadcast consumer 已連線、market-data stores 可供 recovery endpoints 查詢，且 heartbeat 可在本機成功 publish。

Rollback 時，先 drain 新 gateway pool，把新連線 route 回舊 pool，並保持 durable market-data stores 不變。Clients 會透過同一個 recovery cursor contract 重連，因此 rollback 不應要求 order 或 matching worker 更動。
