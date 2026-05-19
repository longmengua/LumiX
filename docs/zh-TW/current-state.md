<!-- 檔案用途：快速說明目前程式碼完成度、MVP 能力與 production blocker。英文版本位於 ../en/current-state.md。 -->
# 目前狀態

這份文件回答一個問題：目前這個 repo 到底完成到哪裡？

結論：目前是可執行、可測試的交易核心 MVP，不是 production-ready 交易所。它已經有核心流程 baseline，但還缺少真實資金與真實流量需要的 durable state、replay、完整對帳、監控與營運控制。

English version: [../en/current-state.md](../en/current-state.md)

## 完成度快照

以下數字來自 [todo.md](todo.md) 的 `[x]` / `[ ]` 狀態。

| 範圍 | 已完成 baseline | 未完成 production 工作 | 判讀 |
| --- | ---: | ---: | --- |
| P0 必做 | 25 | 11 | MVP 核心能力已鋪底，但 production blocker 仍很多。 |
| P1 強烈建議 | 5 | 17 | 營運、market data、Polymarket、資料治理仍偏早期。 |
| P2 演進項 | 0 | 5 | 後台、報表、壓測、合規與灰度能力尚未開始。 |
| 合計 | 30 | 33 | 目前不是接近完成，而是「baseline 已建立、production 化待推進」。 |

## 目前可合理依賴的能力

- 本機可用 Docker Compose 啟動 MySQL、Redis、Kafka、Kafka UI。
- 內部交易所下單鏈路已有 MVP：基本驗證、pre-trade risk、in-memory matching、帳務更新、事件發布。
- 撮合核心已覆蓋 FIFO、post-only、自成交防護、IOC/FOK、市價單流動性不足等 deterministic tests。
- 已有 in-process 單 symbol sequencer baseline，避免同一 symbol 在單進程內並行改狀態。
- 已有 wallet ledger balanced posting baseline，資金變動可在 MVP 內追蹤與測試。
- 已拆出 order reserve、position margin、fee、rebate、realized PnL、funding、liquidation shortfall、deposit、withdrawal 等 accounting entries。
- 已有入金/出金狀態機 baseline，支援 pending、confirmed、failed、reversed、manual review。
- 已有 account risk snapshot、persisted risk snapshot、pre-trade risk checks、risk tiers、global risk switches、mark/index price oracle baseline、liquidation MVP、funding settlement MVP、reconciliation baseline。
- 已有 outbox retry、max retry、DLQ replay、manual compensation baseline。
- 已有 Kafka topic、Redis key schema、request/correlation id、audit log、ops metrics baseline 文件。
- 測試資料夾已有 README 索引，測試案例也用註解和 `@DisplayName` 說明測試鏈路。

## 目前不能當作 production 完成的地方

- 撮合引擎仍是 in-memory，還沒有 durable command log、event log、snapshot、offset checkpoint 與 replay。
- 單 symbol sequencer 只有 in-process baseline，還沒有 production deployment / failover 規則。
- order lifecycle event 已有 durable event log 與最新狀態 projection baseline；更完整的 order/account replay 與營運 runbook 仍未完成。
- ledger 已有 durable double-entry journal 與 replay path；audit retention、更深入 replay validation 與營運控制仍未完成。
- funding、account risk snapshot 與手動 liquidation 已改由 mark/index price oracle 餵價；risk tiers 已涵蓋初始保證金、維持保證金、槓桿與階梯倉位上限。production feed redundancy、price clamp 與 liquidation scanning 仍未完成。
- reconciliation 已有 persisted reports、可設定排程策略、alert-route baseline 與 event-store coverage checks。
- outbox 已使用 MySQL durable store 保存 outbox/DLQ records，並已有 replay/compensation runbook。
- MySQL、Redis、Kafka 之間的 transaction boundary 還沒有完整定義。
- market data 還缺 durable sequence checkpoint、reconnect backfill、ticker/kline/trade tape persistence。
- WebSocket/SSE gateway 還沒有獨立部署、水平擴展、訂閱授權、心跳、限流與斷線補償。
- Polymarket order lifecycle、schema versioning、idempotent commands、user WebSocket worker 還大多是待辦。
- metrics backend、distributed tracing export、dashboard、alerting 還不完整。
- production index、Flyway-only schema policy、archive policy、admin console、報表、壓測、合規能力都還沒完成。

## 建議接下來先做什麼

1. 建立 durable order / ledger / event schemas 與 lifecycle projections。
2. 把 matching engine 演進成可 replay 的核心：command log、event log、snapshot、offset checkpoint。
3. 強化 mark/index price oracle 的 production feed，再補 production pre-trade abuse controls 與 liquidation scanning。
4. 補 reconciliation reports、排程、alert routing，並定義 MySQL / Redis / Kafka transaction boundary。
5. 將 outbox、market data、WebSocket gateway、Polymarket worker 從 MVP baseline 推向可營運版本。

## 閱讀順序

快速掌握狀態請先看本文件，再看：

1. [todo.md](todo.md)：完整 production readiness checklist。
2. [technical.md](technical.md)：技術文件入口。
3. [README.md](README.md)：產品與 API 總覽。
4. [../../src/main/java/com/example/exchange/infra/matching/README_ch.md](../../src/main/java/com/example/exchange/infra/matching/README_ch.md)：撮合引擎現況。
