<!-- 檔案用途：繁體中文任務目錄索引。英文版本位於 ../en/tasks.md。 -->
# 任務目錄

English version：[../en/tasks.md](../en/tasks.md)

任務文件會把 roadmap 工作拆成可點名執行的 Markdown 檔。插單需求進來時，Codex 應先把需求轉成 task md，再等使用者指定要先做哪個任務檔。

## 入口

| 文件 | 說明 |
| --- | --- |
| [Task README](../tasks/README.md) | 任務流程、狀態定義與任務群組。 |
| [Core Kernel README](../tasks/core-kernel/README.md) | 目前核心交易所 / 撮合內核優先線。 |
| [Post-v1 Production Hardening README](../tasks/post-v1/README.md) | core-v1 freeze 後的 production hardening 主線。 |

## 核心內核任務

| 順序 | 任務 | 目的 |
| ---: | --- | --- |
| 1 | [可 Replay 撮合核心](../tasks/core-kernel/01-replayable-matching-core.md) | Durable command/event log、snapshot、checkpoint、replay validation。 |
| 2 | [Liquidation And ADL](../tasks/core-kernel/02-liquidation-adl.md) | Liquidation scanning、execution、ADL ranking、營運控制。 |
| 3 | [體驗金與流水](../tasks/core-kernel/03-bonus-credit-turnover.md) | 體驗金帳務與流水 tracking。 |
| 4 | [可審計帳本對賬](../tasks/core-kernel/04-auditable-ledger-reconciliation.md) | Trial balance、immutable journals、exception workflow。 |
| 5 | [做市商對沖](../tasks/core-kernel/05-market-maker-hedging.md) | 報價、inventory、hedge interface、hedge strategy、audit trail。 |

## Post-v1 Production Hardening

| 順序 | 任務 | 目的 |
| ---: | --- | --- |
| 1 | [交易邊界](../tasks/post-v1/01-transaction-boundaries.md) | 明確 MySQL、Redis、Kafka、撮合、ledger、outbox 的一致性與恢復邊界。 |
| 2 | [Production Worker Routing](../tasks/post-v1/02-production-worker-routing.md) | 將撮合指令導入 sequencer lease guard 與 owner epoch。 |
| 3 | [ADL 強制執行](../tasks/post-v1/03-adl-forced-execution.md) | 將 ADL 從排名/計畫推進到強制減倉、帳務與 audit。 |
| 4 | [行情資料持久化](../tasks/post-v1/04-market-data-durability.md) | 補 depth sequence checkpoint、reconnect backfill、ticker/kline/trade tape durability。 |
| 5 | [外部 API Idempotency](../tasks/post-v1/05-external-api-idempotency.md) | 逐一補齊 timeout、retry、circuit breaker、rate limit 與 idempotency coverage。 |

## Web 任務

| 順序 | 任務 | 目的 |
| ---: | --- | --- |
| 1 | [客戶端 Web](../tasks/web/01-client-web.md) | 交易、帳戶、行情、持倉、訂單、資金與使用者通知。 |
| 2 | [管理端 Web](../tasks/web/02-admin-web.md) | 管理 dashboard、風控控制、對賬、DLQ/recovery、ADL 與做市商營運。 |
