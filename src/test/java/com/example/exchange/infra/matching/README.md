# test infra/matching

In-memory matching engine tests。

目前內容：

| 測試類別 | 主要案例 |
| --- | --- |
| `InMemoryMatchingEngineTest` | 同價位 FIFO、post-only taker 拒絕、自成交拒絕、FOK/IOC、市價單流動性不足、snapshot/replay、cancel-replace command replay。 |
| `MatchingLogOwnerEpochTest` | command/event log owner epoch audit 欄位。 |

注意：
- matching engine 仍是 MVP in-memory core；新增撮合規則時要先補這裡的 deterministic tests。
