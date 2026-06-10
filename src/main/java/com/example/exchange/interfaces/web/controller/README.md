# interfaces/web/controller

REST controllers。

目前重點：
- `OrderController`：下單、查單、撤單、amend、cancel-replace、bulk cancel。
- `MarginController`：入金、入金 callback、出金、manual-review claim、transfer reconciliation、margin transfer、account、ledger、risk snapshot、bonus-credit user/campaign report/export/clawback、turnover summary/records/export/reconciliation。
- `MarketDataController` / `DepthController`：ticker、trades、trade cursor replay、klines、depth、depth delta、depth delta backfill、recovery cursor；stream endpoints 會在 gateway drain 時拒絕新連線。
- `AdminMarketConfigController`：admin market-config list/detail screen data，以及 audited maker/taker fee update endpoint；既有掛單靠 order fee snapshot 不受新費率影響。
- `AdminRiskParametersController`：admin risk switches、symbol risk tiers 與 oracle state read-only screen data。
- `AdminDlqController`：admin DLQ read-only list/detail screen data，payload / headers 只回 sanitized preview。
- `AdminTestFundsController`：MVP 後台測試金發放，註冊後尚未接充值/提幣前由 admin 透過既有 deposit ledger flow 入帳。
- `RiskController`：funding、liquidation、insurance fund、insurance fund movement report、ADL queue inspection、stuck-claim report、open/stuck alert report、claim/release/execution、recent ADL execution report、ADL insurance reconciliation。
- `MarketMakerController`：做市商 profile、risk limit 後台管理、quote placement/active state/reconciliation 查詢與 repair、hedge fill 查詢/venue callback、hedge reconciliation 與手動 hedge execution。
- `RecoveryController`：snapshot recovery、matching worker readiness、account-position restore consistency、reconciliation、ledger replay comparison、daily/category finance report、category export batch、ledger archive delete guard / restore smoke / replay validation、reconciliation issue workflow、outbox DLQ replay/compensation、outbox/domain-state consistency report。
- `PredictionOrderController`：Polymarket order/session/market operations、approval cache、RPC transaction unresolved report，以及 user WebSocket worker status/manual replay。
- `OperationsController`：輕量 metrics 與 push gateway runtime status。

注意：
- controller 只做 HTTP 轉換、validation、呼叫 use case/service。
- 新 endpoint 要同步 docs、curl scripts、security classifier。
