<!-- 檔案用途：繁體中文任務目錄索引。英文版本位於 ../en/tasks.md。 -->
# 任務目錄

English version：[../en/tasks.md](../en/tasks.md)

任務文件會把 roadmap 工作拆成可點名執行的 Markdown 檔。插單需求進來時，Codex 應先把需求轉成 task md，再等使用者指定要先做哪個任務檔。

## 入口

| 文件 | 說明 |
| --- | --- |
| [Task README](../tasks/README.md) | 任務流程、狀態定義與任務群組。 |
| [Core Kernel README](../tasks/core-kernel/README.md) | 目前核心交易所 / 撮合內核優先線。 |

## 核心內核任務

| 順序 | 任務 | 目的 |
| ---: | --- | --- |
| 1 | [可 Replay 撮合核心](../tasks/core-kernel/01-replayable-matching-core.md) | Durable command/event log、snapshot、checkpoint、replay validation。 |
| 2 | [Liquidation And ADL](../tasks/core-kernel/02-liquidation-adl.md) | Liquidation scanning、execution、ADL ranking、營運控制。 |
| 3 | [體驗金與流水](../tasks/core-kernel/03-bonus-credit-turnover.md) | 體驗金帳務與流水 tracking。 |
| 4 | [可審計帳本對賬](../tasks/core-kernel/04-auditable-ledger-reconciliation.md) | Trial balance、immutable journals、exception workflow。 |
| 5 | [做市商對沖](../tasks/core-kernel/05-market-maker-hedging.md) | 報價、inventory、hedge interface、hedge strategy、audit trail。 |
