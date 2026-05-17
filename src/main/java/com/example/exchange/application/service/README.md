# application/service

應用服務層，放跨 use case 共用的業務協調。

目前重點：
- `OrderService`：下單後撮合、持倉、ledger、market data、事件發布。
- `RiskService`：pre-trade checks、reserve、amend reserve reconciliation。
- `WalletLedgerService` / `MarginService`：帳務 posting、入出金狀態機、margin transfer。
- `AccountRiskService` / `ReconciliationService`：帳戶風險快照與對帳 baseline。
- `OutboxService`：retry、DLQ replay、manual compensation baseline。
- `MarketDataService`：ticker、trade tape、kline、depth delta。

目前狀態：
- 這層承擔 MVP orchestration，尚未具備 production transaction boundary。
- 新增服務時要補公開方法註解，說明是否寫狀態、是否可重入、是否依賴外部系統。
