# interfaces/web/controller

REST controllers。

目前重點：
- `OrderController`：下單、查單、撤單、amend、cancel-replace、bulk cancel。
- `MarginController`：入金、入金 callback、出金、manual-review claim、transfer reconciliation、margin transfer、account、ledger、risk snapshot、bonus-credit user/campaign report/export/clawback、turnover summary/records/export/reconciliation。
- `MarketDataController` / `DepthController`：ticker、trades、klines、depth、depth delta、depth delta backfill。
- `RiskController`：funding、liquidation、insurance fund、ADL queue inspection、stuck-claim report、claim/release/execution、recent ADL execution report、ADL insurance reconciliation。
- `MarketMakerController`：做市商 profile、risk limit 後台管理、quote placement/active state/reconciliation 查詢、hedge fill 查詢/venue callback、hedge reconciliation 與手動 hedge execution。
- `RecoveryController`：snapshot recovery、matching worker readiness、account-position restore consistency、reconciliation、ledger replay comparison、daily/category finance report、category export batch、ledger archive restore smoke / replay validation、reconciliation issue workflow、outbox DLQ replay/compensation、outbox/domain-state consistency report。
- `PredictionOrderController`：Polymarket order/session/market operations、approval cache 與 RPC transaction unresolved report。
- `OperationsController`：輕量 metrics。

注意：
- controller 只做 HTTP 轉換、validation、呼叫 use case/service。
- 新 endpoint 要同步 docs、curl scripts、security classifier。
