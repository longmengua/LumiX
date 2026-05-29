# application/usecase

Use case 入口層，對應一個可被 controller 或其他入口觸發的業務流程。

目前重點：
- `PlaceOrderUseCase`：下單入口。
- `CancelOrderUseCase`：單筆/批量撤單入口。
- `AmendOrderUseCase`：maker-only 改單 baseline。
- `CancelReplaceOrderUseCase`：先撤原單再送 replacement baseline。
- `SnapshotRecoverUseCase`、`TransferMarginUseCase`、`LiquidateUseCase`、`ExecuteAdlUseCase` 等。

注意：
- use case 應做 request-level orchestration，不把低層基礎設施細節暴露給 controller。
- 重要流程需發布 lifecycle event 或更新 market data 時，應在這層明確協調。
