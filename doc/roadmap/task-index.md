<!-- 檔案用途：繁體中文任務目錄索引。 -->
# 任務目錄

任務文件會把 roadmap 工作拆成可點名執行的 Markdown 檔。插單需求進來時，Codex 應先把需求轉成 task md，再等使用者指定要先做哪個任務檔。

目前 `doc/tasks/` 細項檔已不在目前 worktree。需要追溯拆分清單時，從 git history 查原本的 unfinished tasks snapshot 或 message-center task。

## 入口

| 文件 | 說明 |
| --- | --- |
| Task README | 任務流程、狀態定義與任務群組；目前檔案已從 worktree 移除。 |
| Unfinished tasks snapshot | 已移除細項檔的狀態快照與任務名稱；目前檔案已從 worktree 移除。 |
| Message center task | 訊息中心後端與前端 API 規格；目前檔案已從 worktree 移除。 |

## 核心內核任務

| 順序 | 任務 | 目的 |
| ---: | --- | --- |
| 1 | 可 Replay 撮合核心 | Durable command/event log、snapshot、checkpoint、replay validation。 |
| 2 | Liquidation And ADL | Liquidation scanning、execution、ADL ranking、營運控制。 |
| 3 | 體驗金與流水 | 體驗金帳務與流水 tracking。 |
| 4 | 可審計帳本對賬 | Trial balance、immutable journals、exception workflow。 |
| 5 | 做市商對沖 | 報價、inventory、hedge interface、hedge strategy、audit trail。 |

## Post-v1 Production Hardening

| 順序 | 任務 | 目的 |
| ---: | --- | --- |
| 1 | 交易邊界 | 明確 MySQL、Redis、Kafka、撮合、ledger、outbox 的一致性與恢復邊界。 |
| 2 | Production Worker Routing | 將撮合指令導入 sequencer lease guard 與 owner epoch。 |
| 3 | ADL 強制執行 | 將 ADL 從排名/計畫推進到強制減倉、帳務與 audit。 |
| 4 | 行情資料持久化 | 補 depth sequence checkpoint、reconnect backfill、ticker/kline/trade tape durability。 |
| 5 | 外部 API Idempotency | 逐一補齊 timeout、retry、circuit breaker、rate limit 與 idempotency coverage。 |

## Web 任務

| 順序 | 任務 | 目的 |
| ---: | --- | --- |
| 1 | 客戶端 Web | 交易、帳戶、行情、持倉、訂單、資金與使用者通知。 |
| 2 | 管理端 Web | 管理 dashboard、風控控制、對賬、DLQ/recovery、ADL 與做市商營運。 |
