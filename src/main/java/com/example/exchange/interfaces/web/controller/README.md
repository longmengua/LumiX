# interfaces/web/controller

REST controllers。

目前重點：
- `OrderController`：下單、查單、撤單、amend、cancel-replace、bulk cancel。
- `MarginController`：入金、出金、margin transfer、account、ledger、transfer、risk snapshot。
- `MarketDataController` / `DepthController`：ticker、trades、klines、depth、depth delta。
- `RiskController`：funding、liquidation、insurance fund、ADL。
- `MarketMakerController`：做市商 profile、risk limit 後台管理、hedge fill 查詢/venue callback、hedge reconciliation 與手動 hedge execution。
- `RecoveryController`：snapshot recovery、matching worker readiness、reconciliation、ledger replay comparison、reconciliation issue workflow、outbox DLQ replay/compensation。
- `PredictionOrderController`：Polymarket order/session/market operations。
- `OperationsController`：輕量 metrics。

注意：
- controller 只做 HTTP 轉換、validation、呼叫 use case/service。
- 新 endpoint 要同步 docs、curl scripts、security classifier。
