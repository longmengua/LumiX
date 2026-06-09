<!-- File purpose: Deployment notes for horizontally scaled WebSocket/SSE market-data gateway instances. -->
# Market Data Gateway Scaling

This note defines the production baseline for running the WebSocket/SSE gateway as an independently deployable edge process. The current code can run the gateway inside the Spring Boot app; production should split gateway instances from order-entry and worker processes when fanout or long-lived connection load grows.

## Runtime Role

Run gateway instances with only HTTP, SSE, WebSocket, auth, stream rate limiting, heartbeat, and market-data read/replay dependencies enabled. Keep order command handling, matching, settlement, and scheduled workers on separate roles.

Each gateway instance owns only its local connected clients. `PushGatewayService` keeps subscriber sessions in memory, so a gateway restart or node loss drops only connections pinned to that node. Clients must reconnect and use the recovery cursor contract instead of relying on server-side session migration.

Configure the runtime role explicitly:

- `push-gateway.runtime.role=${PUSH_GATEWAY_RUNTIME_ROLE:GATEWAY}` for gateway instances.
- `push-gateway.runtime.instance-id=${PUSH_GATEWAY_INSTANCE_ID:${HOSTNAME}}` for logs/readiness.
- `push-gateway.runtime.accept-new-streams=${PUSH_GATEWAY_ACCEPT_NEW_STREAMS:true}` to stop new streams during controlled maintenance.
- `push-gateway.runtime.draining=${PUSH_GATEWAY_DRAINING:false}` to reject new SSE/WebSocket streams with `503` while existing streams drain.

`GET /api/ops/push-gateway/status` returns the instance id, role, accepting/draining flags, active SSE channel/subscriber counts, and active WebSocket channel/session counts.

## Fanout Topology

Use a broadcast feed for public market-data events:

- Matching/trade workers publish depth, trade, ticker, and kline events to Kafka or another shared bus after durable state changes commit.
- Every gateway instance consumes the broadcast feed independently, not as one shared competing consumer group. Each node must receive every public event and filter locally by its subscribed channels.
- If a shared competing consumer group is used, only one gateway node receives a given event and clients connected to other nodes can miss pushes.

Private user streams follow the same local-filtering rule unless a shared subscription registry and targeted routing layer exists. Without that registry, broadcast private events to all gateway instances and filter by authenticated `uid` on the node.

## Load Balancer

Use a load balancer that supports long-lived HTTP connections and WebSocket upgrade:

- Route `/api/market-data/*/stream`, `/api/market-data/user/*/stream`, `/ws/market/*`, and `/ws/user/*` to gateway instances.
- Keep REST order-entry and admin APIs on their own upstream pool when gateway is split.
- Sticky sessions are optional for normal reconnect behavior, but useful for reducing churn. They are not a correctness dependency because clients recover from durable cursors.
- Configure connection draining before deployment or scale-in. Stop accepting new SSE/WebSocket handshakes, keep existing streams until the drain timeout, then close with a reconnectable status.

## Shared Controls

The in-process rate limiter is a safe local baseline. Production multi-instance deployments should choose one of these policies:

- Keep sticky sessions and accept per-node fixed-window limits as a soft control.
- Replace the local counter with Redis or gateway-edge shared counting when limits must be global per client.

Subscription authorization remains mandatory for private streams when `api-auth.enabled=true`. Browser WebSocket clients may pass `apiKey`, `access_token`, or `token` query parameters, but TLS termination and access logs must treat those values as secrets.

## Heartbeat And Recovery

Enable `push-gateway.heartbeat.enabled=true` only on gateway roles. Set `push-gateway.heartbeat.fixed-delay-ms` lower than the client idle timeout and lower than load-balancer idle timeouts.

Clients should treat a missed heartbeat or closed connection as a reconnect signal:

1. Call `GET /api/market-data/{symbol}/recovery-cursor`.
2. Replay depth with `GET /api/market-data/{symbol}/depth-deltas?afterVersion=...`.
3. Replay trades with `GET /api/market-data/{symbol}/trades?afterTs=...&afterMatchId=...`.
4. Resubscribe to SSE/WebSocket streams after replay catches up.

If the replay window was purged by market-data retention, clients must reload a full depth snapshot and continue from the returned version.

## Readiness And Rollback

A gateway instance is ready when auth config is loaded, `GET /api/ops/push-gateway/status` reports `acceptingNewStreams=true`, the broadcast consumer is connected, market-data stores are reachable for recovery endpoints, and heartbeat publication succeeds locally.

For rollback, drain the new gateway pool, route new connections back to the previous pool, and keep durable market-data stores untouched. Clients reconnect through the same recovery cursor contract, so rollback should not require order or matching worker changes.
