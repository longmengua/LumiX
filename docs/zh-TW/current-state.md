<!-- 檔案用途：快速說明目前程式碼完成度、MVP 能力與 production blocker。英文版本位於 ../en/current-state.md。 -->
# 目前狀態

這份文件回答一個問題：目前這個 repo 到底完成到哪裡？

結論：目前是可執行、可測試的交易核心 MVP，不是 production-ready 交易所。目前 roadmap 已改為優先完成核心交易所 / 撮合內核：可 replay 撮合、ADL、體驗金、流水、可審計帳本 / 對賬、做市商對沖。

English version: [../en/current-state.md](../en/current-state.md)

## 完成度快照

以下數字來自 [todo.md](todo.md) 的 `[x]` / `[ ]` 狀態。

| 範圍 | 已完成 baseline | 未完成 production 工作 | 判讀 |
| --- | ---: | ---: | --- |
| P0 必做 | 26 | 17 | MVP 核心能力已鋪底，但 production blocker 仍很多。 |
| P1 強烈建議 | 6 | 16 | 營運、market data、Polymarket、資料治理仍偏早期。 |
| P2 演進項 | 0 | 5 | 後台、報表、壓測、合規與灰度能力尚未開始。 |
| 合計 | 32 | 38 | 目前不是接近完成，而是「baseline 已建立、production 化待推進」。 |

## 目前插單優先順序

接下來應集中在核心交易內核，直到它足以支撐 production-style trading tests：

1. 可 replay 撮合 command/event log、snapshot、checkpoint 與 replay validation。
2. Liquidation 與 ADL 執行鏈路，包含營運控制與 audit event。
3. 體驗金 / bonus credit 帳務與流水 tracking。
4. 可審計 ledger book 與 reconciliation exception workflow。
5. 做市商報價、inventory、kill switch、hedge interface 與 hedge strategy baseline。

Polymarket worker 拆分、WebSocket gateway scaling 與更完整 observability 仍重要，但不能排在這條核心內核優先線前面。

## 目前可合理依賴的能力

- 本機可用 Docker Compose 啟動 MySQL、Redis、Kafka、Kafka UI。
- 內部交易所下單鏈路已有 MVP：基本驗證、pre-trade risk、in-memory matching、帳務更新、事件發布。
- 撮合核心已覆蓋 FIFO、post-only、自成交防護、IOC/FOK、市價單流動性不足等 deterministic tests。
- 已有 in-process 單 symbol sequencer baseline，避免同一 symbol 在單進程內並行改狀態。
- 已文件化單 symbol sequencer ownership 的 production 部署與 failover 規則。
- 撮合狀態已有 in-memory snapshot export/restore baseline，可保留掛單 FIFO 與 match sequence。
- 已有 wallet ledger balanced posting baseline，資金變動可在 MVP 內追蹤與測試。
- 已拆出 order reserve、position margin、fee、rebate、realized PnL、funding、liquidation shortfall、deposit、withdrawal 等 accounting entries。
- 已有入金/出金狀態機 baseline，支援 pending、confirmed、failed、reversed、manual review。
- 已有 account risk snapshot、persisted risk snapshot、pre-trade risk checks、risk tiers、global risk switches、mark/index price oracle baseline、liquidation MVP、funding settlement MVP、reconciliation baseline。
- 已有 outbox retry、max retry、DLQ replay、manual compensation baseline。
- 已有 Kafka topic、Redis key schema、request/correlation id、audit log、ops metrics baseline 文件。
- 測試資料夾已有 README 索引，測試案例也用註解和 `@DisplayName` 說明測試鏈路。

## 目前不能當作 production 完成的地方

- 撮合引擎仍是 in-memory，還沒有 durable command log、event log、offset checkpoint 或完整 replay path。
- 單 symbol sequencer 目前仍只是 in-process 實作，還沒有 production distributed lease、epoch fencing 與 worker routing。
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
- production index、archive policy、admin console、報表、壓測、合規能力都還沒完成。

## 建議接下來先做什麼

1. 把 matching engine 演進成可 replay 的核心：command log、event log、snapshot、offset checkpoint 與 replay validation。
2. 完成 liquidation 與 ADL 執行鏈路與營運控制。
3. 補體驗金 / bonus credit 帳務與流水 tracking。
4. 將 ledger reconciliation 強化成可審計帳本與 exception workflow。
5. 建立做市商 interface 與對沖策略 baseline。

## 閱讀順序

快速掌握狀態請先看本文件，再看：

1. [todo.md](todo.md)：完整 production readiness checklist。
2. [technical.md](technical.md)：技術文件入口。
3. [README.md](README.md)：產品與 API 總覽。
4. [../ai/code-map.md](../ai/code-map.md)：給代理使用的精簡程式碼地圖索引。
5. [../../src/main/java/com/example/exchange/infra/matching/README_ch.md](../../src/main/java/com/example/exchange/infra/matching/README_ch.md)：撮合引擎現況。
