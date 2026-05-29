<!-- 檔案用途：快速說明目前程式碼完成度、MVP 能力與 production blocker。英文版本位於 ../en/current-state.md。 -->
# 目前狀態

這份文件回答一個問題：目前這個 repo 到底完成到哪裡？

結論：目前是可執行、可測試的交易核心 MVP，不是 production-ready 交易所。目前 core-v1 freeze checklist 已關閉，post-v1 production hardening task 也已拆出；下一步是 tag / hand off 有邊界的 core-v1 baseline，然後依 post-v1 task 順序推進 transaction boundaries 等 production blocker。

English version: [../en/current-state.md](../en/current-state.md)

## 完成度快照

以下數字來自 [todo.md](todo.md) 的 `[x]` / `[ ]` 狀態。

| 範圍 | 已完成 baseline | 未完成 production 工作 | 判讀 |
| --- | ---: | ---: | --- |
| P0 必做 | 31 | 12 | MVP 核心能力已鋪底，但 production blocker 仍很多。 |
| P1 強烈建議 | 8 | 14 | 營運、market data、Polymarket、資料治理仍偏早期。 |
| P2 演進項 | 0 | 5 | 後台、報表、壓測、合規與灰度能力尚未開始。 |
| 合計 | 39 | 31 | 目前不是接近完成，而是「baseline 已建立、production 化待推進」。 |

## 目前插單優先順序

接下來應先 tag 或 hand off [core-v1-release-checklist.md](core-v1-release-checklist.md) 所定義的 baseline。Freeze checklist、smoke runbook 與 [post-v1 production hardening tasks](../tasks/post-v1/README.md) 已完成拆分；在 core-v1 tag 之前，不擴 web、Polymarket、報表、合規或觀測範圍。

凍結的 core-v1 baseline 包含：

1. 可 replay 撮合 command/event log、snapshot、checkpoint 與 replay validation。
2. Liquidation 與 ADL 執行鏈路，包含營運控制與 audit event。
3. 體驗金 / bonus credit 帳務與流水 tracking。
4. 可審計 ledger book 與 reconciliation exception workflow。
5. 做市商報價、inventory、kill switch、hedge interface 與 hedge strategy baseline。

Polymarket worker 拆分、WebSocket gateway scaling 與更完整 observability 仍重要，但延後到 core v1 之後。

## 目前可合理依賴的能力

- 本機可用 Docker Compose 啟動 MySQL、Redis、Kafka、Kafka UI。
- 內部交易所下單鏈路已有 MVP：基本驗證、pre-trade risk、in-memory matching、帳務更新、事件發布。
- 撮合核心已覆蓋 FIFO、post-only、自成交防護、IOC/FOK、市價單流動性不足等 deterministic tests。
- 已有 in-process 單 symbol sequencer baseline，避免同一 symbol 在單進程內並行改狀態。
- 已文件化單 symbol sequencer ownership 的 production 部署與 failover 規則。
- 撮合狀態已有 in-memory snapshot export/restore baseline，可保留掛單 FIFO、command offset、event offset 與 match sequence。
- 撮合已有 in-memory command/event log 與 replay baseline，可在 deterministic tests 中從 snapshot checkpoint 重建狀態。
- 撮合 command/event log、engine snapshot 與 replay validation report 也已有 Flyway schema、JPA durable adapter baseline 與 per-symbol offset checkpoint。
- 撮合 recovery orchestration 可從 latest snapshot 加 command log 恢復單一 symbol，並保存恢復後 snapshot 與 replay validation report。
- 撮合 sequencer lease service 可 acquire、renew、release，並在 symbol owner takeover 時遞增 epoch。
- 撮合 sequencer write guard 會在 command write 前拒絕 missing lease、wrong owner、stale epoch 與 expired lease。
- 撮合 command replay 支援帶 replacement order payload 的 cancel-replace。
- 撮合 command/event log entries 可保存 sequencer owner id 與 epoch，供 fencing audit。
- 撮合 worker startup / renewal lifecycle 已可取得 configured symbol lease、執行 recovery、驗證 replay、保存 owner/epoch readiness context、續租、在續租失敗時移除 readiness，並提供 readiness inspection endpoint。
- 撮合 worker execution baseline 已可對 submit、cancel、amend、cancel-replace 先 append lease-fenced command，再套用到 engine，並保留 owner/epoch。
- 既有 submit、cancel、amend、cancel-replace accounting-safe cancel + replacement-submit intake path 在該 symbol 有 ready owner context 時，已可使用 worker execution。
- `matching-worker.fence-legacy-routing` 可在切流時拒絕 configured symbol fallback 到尚未 worker-ready 的舊 in-process path。
- `MatchingWorkerStartupListener` 會在 `matching-worker.enabled=true` 時，於 application ready 後啟動 configured worker symbols。
- 撮合 replay validation 可將 replay output 與 expected snapshot 比對，並回報 command-offset、event-offset、match-sequence 與 book-level 差異。
- Market data depth delta 已有 durable sequence/checksum checkpoints、啟動時恢復最新 depth sequence、durable depth delta records，以及依 known version 查詢後續 deltas 的 reconnect backfill endpoint。Trade tape 也有 restart-safe durable baseline。
- 已有 wallet ledger balanced posting baseline，資金變動可在 MVP 內追蹤與測試。
- 已拆出 order reserve、position margin、fee、rebate、realized PnL、funding、liquidation shortfall、deposit、withdrawal 等 accounting entries。
- 體驗金已用 `USER_BONUS_AVAILABLE` 拆出 grant、consume、expire、clawback ledger postings，且不會改動真實現金帳戶餘額；目前也有 grant 批次 expiry/remaining tracking。
- 流水已有 durable read model baseline，會由已處理成交事件產生 user、account、symbol、strategy、market-maker、order、match、sequence、quantity、price、notional 維度。
- Trial balance 已可從 wallet ledger postings 依 asset/account code 計算。
- 已有入金/出金狀態機 baseline，支援 pending、confirmed、failed、reversed、manual review；入金 callback 可用 `externalRef` 冪等 replay，manual-review transfer 可被 owner claim，transfer reconciliation projection 可用 ledger ref 比對每筆 transfer。
- 已有 account risk snapshot、persisted risk snapshot、pre-trade risk checks、uid+symbol order-entry frequency limits、risk tiers、global risk switches、mark/index price oracle baseline、liquidation MVP、funding settlement MVP、reconciliation baseline。
- liquidation decision 已會發布 audit data，營運控制可 halt liquidation 或導入 manual review。
- liquidation scanning 可掃描 open positions 並觸發 oracle-based liquidation decisions。
- ADL 已有 deterministic ranking、deleveraging-plan、forced-execution、durable queue storage、依 `liquidationId` 冪等的 queue enqueue、queue-to-execution orchestration、operator claim/release、stuck-claim operator reporting、partial retry 與 no-eligible-candidate retry baseline，可減掉選中的持倉、寫入 realized-PnL / socialized-loss ledger postings、發布 audit event，並持久化 execution summary / idempotency records。
- 已有 outbox retry、max retry、DLQ replay、manual compensation baseline。
- 已有 Kafka topic、Redis key schema、request/correlation id、audit log、ops metrics baseline 文件。
- Redis hot-state keys 已補 final per-key-family TTL/archive rules、刪除前置條件與 authoritative rebuild source，供 production maintenance 使用。
- 歷史訂單、成交、ledger entries、Kafka events 與 audit logs 已有 archive strategy，涵蓋 manifests、retention classes、刪除前置條件與 restore rules。
- Order lifecycle projection 現在會輸出可用 uid、orderId、clientOrderId、symbol 搜尋的 `CORE_EVENT` structured log line。
- 測試資料夾已有 README 索引，測試案例也用註解和 `@DisplayName` 說明測試鏈路。

## 目前不能當作 production 完成的地方

- Production worker routing 已有 production deployment switch sequence、readiness inspection、rollback sequence 與聚焦 smoke verification。單 symbol sequencer 目前仍以 in-process engine 執行，因此更完整的 disaster recovery 與多進程營運強化仍未完成。
- order lifecycle event 已有 durable event log 與最新狀態 projection baseline；更完整的 order/account replay 與營運 runbook 仍未完成。
- ledger 已有 durable double-entry journal、體驗金獨立帳戶、體驗金到期 scanner baseline、流水 facts 與 replay path；audit retention、更深入 replay validation、體驗金資格/報表、流水對帳與營運控制仍未完成。
- funding、account risk snapshot 與手動 liquidation 已改由 mark/index price oracle 餵價；risk tiers 已涵蓋初始保證金、維持保證金、槓桿與階梯倉位上限。liquidation scanning 可把 open positions 透過 oracle-based liquidation routing 處理，並具備 halt / manual-review controls、batch limit、per-position failure isolation 與 decision audit events。production feed redundancy、price clamp、stuck claim 的 alert-backend delivery 與 production insurance-fund capital movement records 仍未完成。
- reconciliation 已有 persisted reports、可設定排程策略、alert-route baseline、event-store coverage checks、trial-balance 計算、結構化 ledger replay comparison、issue status/owner/resolved_at workflow 欄位、後台 issue workflow API 與 workflow audit events；daily finance reports 仍未完成。
- 做市商對沖已有 durable profile/risk-limit storage、profile admin API、hedge fill query API、venue fill callback ingestion 與 venue fill idempotent replay、manual 與預設關閉的 scheduled hedge execution API、exposure aggregation、inventory-aware reduce-only hedge planning/execution、global hedge execution halt、per-run execution route cap policy、quote command validation、hedge venue adapter contract、retryable venue result classification、durable refId idempotency claim/result storage、未解 hedge venue idempotency 營運報告、retry/backoff/throttle decorator baselines、standardized venue fill mapping、預設安全拒絕 adapter、hedging risk checks、slippage rejection、quote/hedge decision audit events、durable hedge decision/fill audit trails 與 decision-vs-fill hedge reconciliation；真實 venue adapter、uncertain outcome 的 venue lookup/reconciliation、quote lifecycle integration、production callback authentication/verification、trade/ledger hedge reconciliation、scheduler/worker locking 與 global limits 仍未完成。
- outbox 已使用 MySQL durable store 保存 outbox/DLQ records，並已有 replay/compensation runbook。
- Database indexing 已有 Flyway baseline，涵蓋 durable order lifecycle projection/event、ledger entries/postings、outbox/DLQ/matching events 與 prediction order/user-event 查詢；live order/position hot-state 仍由 Redis 擁有，關閉 TODO 前還需要 durable indexing 設計。
- MySQL、Redis、Kafka 之間已有 order commands、liquidation、ADL execution 與 hedge execution 的 command-boundary/outbox baseline，但仍需要 persistence-backed rollback tests 與更完整的 cross-store failure drills。
- market data 已有 durable depth sequence checkpoints、reconnect backfill depth deltas、durable trade tape、durable ticker latest state、durable 1m klines，以及預設關閉的高流量 depth/trade/kline history DB retention windows。
- WebSocket/SSE gateway 還沒有獨立部署、水平擴展、訂閱授權、心跳、限流與斷線補償。
- Polymarket CLOB place 已有 `clientRequestId` local idempotency baseline，CLOB cancel 可使用 durable `commandId` records，也會對已記錄的 cancel/uncertain 狀態做 local replay，reconcile 可用遠端 CLOB status 解除 uncertain cancel，sync/reconcile 會跳過未變更 local writes，state-machine guard 會防止 stale active CLOB payload 降級 local filled/settled terminal order 或 matched size，approval reads 已有 TTL cache coverage，session signer lifecycle guard 已覆蓋 expiration / revocation / abnormal-use warning，user-channel callback 會對 duplicate `eventKey` replay 與 save-race duplicate 做 no-op，backend-observed RPC transaction 也已有 durable command / txHash tracking envelope 與 unresolved outcome report；更完整的 trade/settlement lifecycle、schema versioning、獨立部署的 user WebSocket worker 還大多是待辦。
- metrics backend、distributed tracing export、dashboard、alerting 還不完整。
- archive exporter jobs、admin console、報表、壓測、合規能力都還沒完成。

## 建議接下來先做什麼

1. Tag 或 hand off 有邊界的 core-v1 baseline。
2. 依 [post-v1 production hardening tasks](../tasks/post-v1/README.md) 推進 P0 production hardening。
3. 優先處理 transaction boundaries、ADL forced execution、market data durability 與外部 API idempotency。
4. 新產品面延後到 core-v1 tag 之後。

## 閱讀順序

快速掌握狀態請先看本文件，再看：

1. [core-v1-release-checklist.md](core-v1-release-checklist.md)：release freeze boundary 與 gates。
2. [core-v1-smoke-runbook.md](core-v1-smoke-runbook.md)：smoke verification flow。
3. [todo.md](todo.md)：完整 production readiness checklist。
4. [technical.md](technical.md)：技術文件入口。
5. [../ai/code-map.md](../ai/code-map.md)：給代理使用的精簡程式碼地圖索引。
