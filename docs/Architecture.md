
## 🏛️ 系統架構圖（High-Level System Architecture）

```
                        ┌─────────────────────────────────────┐
                        │               Clients               │
                        │  Web / iOS / Android / Market Maker │
                        └───────────────┬─────────────────────┘
                                        │ HTTPS / WebSocket
                               ┌────────▼────────┐
                               │   API Gateway   │  (AuthN/Z, Rate-limit, Routing)
                               └───────┬─────────┘
       ┌────────────────────────────────┼────────────────────────────────┐
       │                                │                                 │
┌──────▼───────┐                 ┌──────▼───────┐                  ┌──────▼────────┐
│   Auth/KYC   │                 │   Order App  │                  │  Admin Console│
│(JWT, API Key │                 │(REST/WS, OCO,│                  │(Ops, Config,  │
│  Risk Score) │                 │ Trigger Mgmt)│                  │  Audit UI)    │
└──────┬───────┘                 └──────┬───────┘                  └──────┬────────┘
       │                                │                                   │
       │                                │ place/cancel/query                │
       │                       ┌────────▼─────────┐                         │
       │                       │   Risk-Margin    │  (pre-check, limits)    │
       │                       └────────┬─────────┘                         │
       │ reserve/release funds           │ ok/reject                        │
┌──────▼────────┐                 ┌──────▼────────┐                  ┌──────▼──────────┐
│ Wallet-Ledger │                 │  Matching Core│  (per-symbol     │  Symbol-Config  │
│(double-entry  │<──reserve/settle│ (sequencer,   │   orderbook)     │(tick, lot, fees,│
│  accounting)  │                 │  price/time   │                  │  risk tiers...) │
└──────┬────────┘                 │  priority)    │                  └──────┬──────────┘
       │   trades, fills          └──────┬────────┘                         │
       │   fees, rebates                 │ trades, book deltas               │
┌──────▼─────────┐                 ┌─────▼──────────┐                 ┌──────▼───────────┐
│ Position & PnL │<────trades──────│ Market-Data    │──WS──> Clients  │ Price-Feed/Index │
│(avg, upnl/rpnl │                 │(depth, ticker, │                 │(Index, MarkPrice │
│ funding, stats)│                 │ kline, mark)   │                 │  outlier filter) │
└──────┬─────────┘                 └────────────────┘                 └──────┬───────────┘
       │  mark price, funding rate updates                                 │ index/mark updates
       │                                                                    │
       │                                                                    │
       │   ┌────────────────────────────────────────────────────────────────▼────────────┐
       │   │                                     Kafka (Events)                           │
       │   │ order.*, trade.executed, position.changed, mark_price.updated, funding.*,    │
       │   │ liquidation.*, wallet.*, snapshot.*                                          │
       │   └───────────────────────────────┬──────────────────────────────────────────────┘
       │                                   │
┌──────▼───────────┐                ┌──────▼──────────┐                 ┌────────▼─────────┐
│ Reporting-OLAP   │<== stream ==>  │   ETL/Replay    │  ==> S3/Blob    │ Snapshot Service │
│ (ClickHouse)     │                │(Kafka Consumer, │  archive,        │ (make/restore)   │
│ BI/Reports/Alerts│                │  CDC, validators│  reprocess)      └────────┬─────────┘
└──────┬───────────┘                └─────────────────┘                         │
       │ SQL (ad hoc / APIs)                                                   │ restore from
       │                                                                        │ snapshot + events
┌──────▼───────────┐     ┌───────────────┐     ┌───────────────┐         ┌──────▼───────────┐
│ MySQL (OLTP)     │     │ Redis (cache) │     │ Timeseries/TS │(opt.)   │  Object Storage  │
│ orders, trades,  │     │ sessions,     │     │ (metrics)     │         │  (S3, snapshots) │
│ positions, ledger│     │ rate limits   │     │               │         └──────────────────┘
└──────────────────┘     └───────────────┘     └───────────────┘
```
