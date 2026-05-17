# test application/service

Application service tests。

目前內容：

| 測試類別 | 主要案例 |
| --- | --- |
| `OrderAccountingIntegrationTest` | 成交後帳務與持倉、風控拒單、max open orders、重複 client order id、kill switch、批量撤單、改單、cancel-replace、cancel-on-disconnect。 |
| `RiskSettlementServiceTest` | 單一 funding、批次 funding、強平與保險基金、全帳戶 reconciliation。 |
| `OutboxServiceTest` | publish 失敗 retry、DLQ、replay、manual compensation、trace header 傳遞。 |
| `MarginServiceTest` | 入金、成功出金、出金暫停進人工覆核、餘額不足拒絕。 |
| `AccountRiskServiceTest` | 帳戶不存在零值快照、mark price 下的 equity/maintenance/risk ratio。 |
| `OperationalMetricsServiceTest` | 下單結果 counters、取消數、成交事件數、latency 統計。 |

注意：
- 測試使用 in-memory repository stub；行為重點是 business flow，不是 Redis/JPA。
