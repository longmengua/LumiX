## 🏛️ DFD（Data Flow Diagram）

```
                 ┌───────────────────┐
                 │   Market Makers   │
                 └─────────┬─────────┘
                           │ Quotes / Orders
                           │
┌──────────────┐    ┌──────▼────────┐    ┌───────────────────┐
│   Traders    │───▶│ Perpetual     │◀──▶│ Market Data Feeds │
│(Web/App/API) │    │ Exchange      │    │ (Exchanges, Oracles)
└──────┬───────┘    │ (System)      │    └───────────────────┘
       │            └──────┬────────┘
       │                   │
       │   Reports/API     │ Events / Data
       │                   │
 ┌─────▼───────────┐    ┌──▼─────────────┐   ┌──────────────────┐
 │ Admin/Compliance│    │ Datastores     │   │  Object Storage  │
 │ (Ops/KYC/Audit) │    │ (MySQL, CH,    │   │  (Snapshots)     │
 └─────────────────┘    │  Kafka, Redis) │   └──────────────────┘
                        └────────────────┘
```