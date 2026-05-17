# test application/service

Application service tests。

目前內容：
- `OrderAccountingIntegrationTest`：下單、成交、帳務、改單、撤單、cancel-on-disconnect。
- `RiskSettlementServiceTest`：funding、liquidation、reconciliation。
- `OutboxServiceTest`：retry、DLQ、replay、manual compensation。
- `MarginServiceTest`：deposit/withdraw transfer state machine。
- `AccountRiskServiceTest`：risk snapshot。
- `OperationalMetricsServiceTest`：metrics counters。

注意：
- 測試使用 in-memory repository stub；行為重點是 business flow，不是 Redis/JPA。
