<!-- 檔案用途：繁體中文 production TODO；其他語言請見根目錄 README.md。 -->
# Production TODO

這份清單聚焦「要把目前 MVP 推向 production」前應補齊的能力。目前 core-v1 freeze 已關閉；接下來以 [post-v1 production hardening tasks](../tasks/post-v1/README.md) 作為核心 production 工作主線。粗項 TODO 的細分進度追蹤放在 [production-readiness fine tasks](../tasks/production-readiness/README.md)。

文件分類：[產品文件](README.md) / [技術文件](technical.md) / 待辦清單文件

## 目前 Freeze 工作

- [x] 關閉 [core-v1-release-checklist.md](core-v1-release-checklist.md)。
- [x] 執行 [core-v1-smoke-runbook.md](core-v1-smoke-runbook.md)。
- [x] 只修 freeze verification 發現的 compile/test/checklist 缺口。
- [x] Web app、Polymarket production worker split、完整報表、合規與觀測擴建延後到 core-v1 tag 之後。

## P0 必做

### 核心交易內核優先線

- [x] 完成可 replay 的撮合核心：durable command log、event log、snapshot、offset checkpoint 與 deterministic replay validation。
  - Baseline 已完成：durable command/event logs、durable snapshots、offset checkpoints、owner/epoch fencing audit fields、deterministic replay validation reports、worker startup/takeover recovery orchestration、recovered open orders restore drill coverage，以及 interleaved command offsets 的 multi-symbol replay validation。
- [x] 完成 production ADL：隊列排序、強制減倉執行、audit event、保險基金互動與營運控制。
  - Baseline 已完成：deterministic ranking/planning、liquidation decision audit、營運 halt/manual-review hooks、forced-execution service、持倉減倉、ledger postings、audit event、durable execution summary/idempotency records、recent execution report API、durable ADL queue store、依 `liquidationId` 冪等的 ADL queue enqueue、queue-to-execution orchestration、operator claim/release guard、stuck/open ADL alert report API、stuck-claim operator report 與 runbook、含 restart coverage 的 partial-execution retry semantics、no-eligible-candidate retry semantics、ADL insurance/shortfall reconciliation，以及 durable insurance-fund movement records。
- [x] 補體驗金 / bonus credit 帳務：獨立 ledger account、資格規則、扣抵順序、到期、追回與報表。
  - Baseline 已完成：獨立 bonus ledger account、grant/consume/expire/clawback postings，不混入真實現金餘額，並有 grant 批次 remaining tracking、預設關閉的 expiry scanner、可設定 consume eligibility gate、預設關閉的 campaign auto-clawback policy、用戶/活動體驗金 report APIs、可匯出的 campaign rows 與營運 clawback API。
- [x] 補流水 tracking：按 user、account、symbol、strategy、market-maker 維度統計，並能與 ledger/trade reconciliation 對齊。
  - Baseline 已完成：已處理成交會寫 durable turnover records，strategy/market-maker 一等 order tags 會通過下單與 lifecycle projection，並有 uid/symbol/strategy/market-maker/match 維度的 turnover summary、限量 drill-down、export rows、match-level trade-tape reconciliation APIs，以及預設關閉的 recent-window uid+match trade tape / ledger-ref reconciliation。
- [x] 將 ledger reconciliation 強化成可審計帳本：immutable journal、trial balance、replay comparison、exception workflow 與財務報表。
  - Baseline 已完成：durable journal hash-chain tamper-evidence、SQL-enforced wallet ledger entry/posting invariants、trial-balance 計算與 daily snapshot persistence、結構化 replay comparison、reconciliation issue workflow 欄位/後台 API、workflow audit events、按 reason/asset/account code 彙總 durable ledger 的 daily finance report、預設關閉的 finance category exporter job、fee/funding/liquidation/bonus/transfer category exports、ledger archive/delete eligibility checks、ledger archive manifest checksums、restore smoke checks、archived date ranges replay validation，以及不平衡報表 operator runbook。
- [ ] 建立做市商 interface：報價、inventory、risk limit、kill switch 與 hedge order routing。
  - Baseline 已完成：durable profile/risk-limit storage、admin profile API、exposure aggregation、quote command validation、stale quote cleanup、post-only quote order placement、durable active quote state/operator lookup、per-side quote version metadata、active quote reload coverage、quote/open-order reconciliation、kill switch、slippage control、hedge venue contract、預設安全拒絕 adapter、real venue signed-request/lookup skeleton、quote/hedge decision audit events、含 internal trade ref 的 durable hedge decision audit trails、含 ledger ref 的 hedge fill audit persistence，以及 hedge trade/ledger reconciliation issues。
- [ ] 建立做市商對沖策略 baseline：exposure aggregation、hedge venue adapter interface、execution policy、slippage controls 與 hedge audit trail。
  - Baseline 已完成：exposure aggregation、hedge venue adapter interface、real venue signed-request/lookup skeleton、enabled-profile batch 共用的 per-run execution route/notional cap policy、slippage controls、durable scheduled-worker lock、operator approval token gate、bounded operator queries/ref prefixes、可選 venue callback HMAC/timestamp verification、uncertain outcome venue lookup/reconcile contract、stale quote cleanup、post-only quote order placement、durable active quote state/operator lookup、per-side quote version metadata、quote/open-order reconciliation、含 internal trade ref 的 durable hedge decision audit trail、含 ledger ref 的 fill audit persistence，以及 trade/ledger reconciliation issues；real venue HTTP transport 與 automated quote reconciliation repair job 仍待補。

### 交易與撮合

- [x] 將 in-memory matching engine 演進為可 replay 的撮合核心，至少要有 command log、event log、snapshot、offset checkpoint。
- [x] 補上 in-process 單 symbol sequencer baseline，讓同一 symbol 的撮合操作序列化。
- [x] 定義 production 單 symbol sequencer 的部署與 failover 策略，避免多實例同時處理同一 symbol 造成狀態分裂。
- [x] 發布 order lifecycle events，涵蓋 created、accepted、updated、rejected、canceled、expired、filled。
- [x] 將 order lifecycle events 產品化，補 durable storage、schema version、replay 與查詢 projection。
- [x] 補上 amend order、cancel replace、bulk cancel、cancel on disconnect 的 REST/WebSocket baseline。
- [x] 為交易所常見指令補 durable command log、更強 atomicity mode 與 reconnect/session 語意。
  - Baseline 已完成：durable matching command/event logs、worker fencing、cancel-replace command replay、cancel-replace reserve-release/replacement rollback coverage、cancel-on-disconnect connection resume semantics，以及 DR runbook reconnect/session replay guidance。
- [x] 在 pre-trade checks 落實 tick size、lot size、min notional、price band、max order size、max open orders。
- [x] 明確處理 MARKET 流動性不足、IOC/FOK 未完全成交、POST_ONLY 會吃單、REDUCE_ONLY 超過可減倉量等拒單原因。

### 帳務與資金

- [x] 建立 wallet ledger balanced posting baseline，讓 MVP 資金變動可追溯、可對帳。
- [x] 建立完整 production 雙分錄 ledger schema 與 replay path。
- [x] 將 order reserve、position margin、fee、rebate、realized PnL、funding、liquidation shortfall、deposit、withdrawal 拆成明確 accounting entries。
- [x] 用 production database constraints、audit retention 與 replay validation 強化 accounting entries。
  - Baseline 已完成：SQL-enforced wallet ledger entry/posting constraints、durable hash-chain tamper evidence、daily/category finance reports、trial-balance snapshots、replay comparison、archive eligibility、archive manifest checksums、restore smoke、archived date-range replay validation，以及 immutable ledger archive delete guard，會在 eligibility、manifest、restore smoke、replay validation 未全數通過時阻擋 hot-path delete。
- [x] 補上 `/api/margin/risk`，計算凍結資金、可用餘額、總權益、維持保證金與風險率 snapshot。
- [x] 持久化每日 account risk snapshots，並完整用獨立 mark/index oracle inputs 取代 trade/ticker fallback marks。
- [x] 補上全帳戶對帳 baseline，掃 maintained account index 與 open-position index，回報 account、position margin、ledger balance 問題。
- [x] 補 persisted reconciliation reports、排程策略、alert routing 與 event-store coverage。
- [x] 補上 Redis-backed 入金/出金狀態機 baseline，支援 pending、confirmed、failed、reversed、manual review transfer states。
- [x] 補鏈上/銀行 callbacks、人工覆核流程 owner 與 transfer reconciliation projections。
  - Baseline 已完成：入金 callback 使用 `externalRef` 冪等 replay，避免重送時重複寫 ledger；manual-review transfer 可被 owner claim；transfer reconciliation projection 會用 wallet ledger ref 比對每筆 transfer。

### 風控

- [x] 接入 mark price / index price oracle，避免 liquidation 與 funding 使用成交價或任意輸入價。
- [x] 建立 symbol risk baseline 設定：最大槓桿、維持保證金率、最大倉位名義金額、最大 open orders。
- [x] 補完整 risk tier：初始保證金率與階梯倉位上限。
- [x] 補上 pre-trade risk checks：餘額、槓桿、倉位、敞口、價格偏離、client order id 去重。
- [x] 為 pre-trade risk checks 補 production frequency limits 與更完整 abuse controls。
  - Baseline 已完成：可設定 uid+symbol fixed-window order-entry frequency limit，會在 reserve / matching side effect 前拒絕 burst orders；預設關閉，多實例 production 可替換為 Redis / gateway shared counting。
- [x] 補上 liquidation MVP，涵蓋觸發、平倉、保險基金、ADL 與 audit event。
- [x] 補 production liquidation scanning、execution routing 與 operational controls。
  - Baseline 已完成：scanner 會掃描 open positions 並走 oracle-based liquidation routing，支援 operator halt / manual-review controls、decision audit events、可設定 scan batch size，以及 per-position failure isolation，避免單一壞 symbol/config 中止整批掃描。
- [x] 加上全站風控開關：只減倉、禁止下單、禁止提現、指定 symbol 停牌。

### 可靠性與一致性

- [x] 補上 outbox retry backoff、最大重試、DLQ replay 與人工補償流程 baseline。
- [x] 將 outbox 移到 production durable storage，並補人工補償 runbook。
- [x] 文件化 Kafka topic partition key、retention、compaction、schema version 與 consumer group 策略。
- [x] 補上外部 API 共用 HTTP timeout、retry、circuit breaker 與 rate-limit baseline。
- [x] 逐一確認所有外部 API 都具備 timeout、retry、circuit breaker、rate limit 與 idempotency coverage。
  - Baseline 已完成：external API inventory、共用 HTTP timeout/retry/circuit/rate-limit config、使用 `refId` 的 durable hedge venue submit idempotency envelope、使用 `venueOrderId + venueFillId` 的 hedge venue fill callback replay、可選 hedge venue fill callback HMAC/timestamp verification、未解 hedge venue idempotency 營運報告與 lookup reconcile trigger、使用 `clientRequestId` 的 CLOB place local idempotency、durable CLOB cancel `commandId`、CLOB cancel 對已記錄 cancel/uncertain 狀態的 local replay、uncertain cancel 的 reconcile resolution、CLOB sync/reconcile 對未變更 payload 的 no-op local replay、使用 `eventKey` 的 Polymarket user-channel callback replay / race idempotency、approval read TTL cache coverage，以及 durable backend-observed RPC transaction tracking / unresolved outcome report。
- [x] 所有核心寫入需要明確交易邊界；MySQL、Redis、Kafka 之間不能假設天然一致。
  - Baseline 已完成：command transaction boundaries 已包住 order place/cancel/amend/cancel-replace、manual liquidation、ADL forced execution 與 hedge execution；outbox row 會在 command transaction 內保存，外部 publish 延到 `afterCommit`；rollback coverage 已包含 order-place outbox insert failure、cancel ledger-release failure、cancel-replace reserve-release/replacement reserve failure、hedge audit/outbox failure；並有 cross-store MySQL/Redis/Kafka failure drill 與 outbox/domain-state consistency recovery report。
- [x] 補上 MVP snapshot + event replay recovery 入口。
- [x] 建立 production 災難恢復流程：從 snapshot + event log 恢復 matching/order/account/position。
  - Baseline 已完成：matching/order/account/position restore production DR runbook、worker takeover steps、restore smoke command list、outbox/domain-state consistency report，以及 restore 後 account/position consistency validation report。

### 安全

- [x] Session signer lifecycle 要有過期、撤銷、審計、異常使用檢測。
  - Baseline 已完成：session 使用會拒絕 inactive / expired record，過期使用會把 record 標成 `EXPIRED`，revoke-all 會同時覆蓋 `PENDING` 與 `ACTIVE`，避免已簽發但未 confirm 的 signer 在 wallet-wide revoke 後又變 ACTIVE；limit breach / invalid-use warning 也提供異常使用追查的 audit trail。

## P1 強烈建議

### Market Data

- [x] 補上 REST/SSE depth delta，帶 monotonic version 與 CRC32 checksum，支援 snapshot + delta 校驗。
- [x] 補 durable sequence checkpoint 與 reconnect backfill。
  - Baseline 已完成：durable depth-delta sequence/checksum checkpoints、duplicate/out-of-order checkpoint ignore、啟動時恢復最新 depth sequence、durable depth delta records、`GET /api/market-data/{symbol}/depth-deltas?afterVersion=...` reconnect backfill、durable trade tape records、durable ticker latest-state records、durable 1m kline records，以及預設關閉的 DB retention windows。
- [x] 定義高流量 market-data depth、trade 與 kline history 的 retention/archive policy。
  - Baseline 已完成：DB retention job 依獨立 window 清理 depth delta、trade tape 與 1m kline history；production archive export/storage 仍屬於後續營運任務。
- [ ] WebSocket/SSE gateway 獨立部署，支援水平擴展、訂閱權限、心跳、限流、斷線補償。
- [ ] 在 P0 做市商 interface baseline 完成後，補齊 market maker / liquidity provider API hardening 與節流策略。

### Polymarket 整合

- [ ] 建立 Polymarket order 狀態機，完整追蹤 local order、CLOB order、trade、settlement lifecycle。
- [ ] 將 Gamma/CLOB response schema version 化，避免遠端欄位變更造成解析錯誤。
- [x] 對 CLOB 下單、取消、同步、reconcile 做 idempotent command 設計。
  - Baseline 已完成：place 可用 `clientRequestId`；cancel 可用 durable `commandId`；cancel 會 local replay 已記錄的 cancel/uncertain 狀態；reconcile 可用遠端 CLOB status 解除 uncertain cancel；sync/reconcile 會跳過未變更 local writes；stale active CLOB payload 不會降級 local filled/settled terminal order 或 matched size。Remaining：完整 trade/settlement state-machine transitions。
  - Baseline 已完成：place 支援 `clientRequestId` duplicate replay、payload conflict rejection 與 uncertain local-order retry blocking。
- [ ] User WebSocket 服務獨立部署，支援自動重連、checkpoint、事件去重、落庫與 replay。
- [x] allowance / approval 查詢加入 cache 與過期策略，避免 RPC 被打爆。
  - Baseline 已完成：ERC20 allowance 與 ERC1155 approval reads 使用 owner/contract scoped TTL cache，支援 owner-scoped clear、full-cache clear 與 expiry refresh 測試覆蓋。

### 資料庫與儲存

- [ ] 為 orders、positions、ledger、events、prediction orders 補齊 production index。
  - Baseline 已完成：Flyway `V12__production_query_indexes.sql` 已補 durable order lifecycle projection/event、ledger entries/postings、outbox/DLQ/matching events、prediction orders/user events 的 query indexes。Remaining：live order/position hot-state 從 Redis-owned model 搬移或 mirror 到 durable table 後，仍需補正式 SQL indexes。
- [x] 文件化 Redis key schema、namespace prefix、版本與 migration 策略。
- [x] 補 Redis hot-state key 的最終 TTL / archive rules。
  - Baseline 已完成：`docs/zh-TW/redis-key-schema.md` 已按 key family 定義 account、position、order、snapshot、ledger、outbox/DLQ、idempotency keys 的 production TTL、archive/delete rule 與 authoritative rebuild source。
- [x] Flyway migration 改為正式唯一 schema 管理，不再依賴 Hibernate `ddl-auto=update`。
- [x] 補齊資料歸檔策略：歷史訂單、成交、ledger、Kafka event、audit log。
  - Baseline 已完成：`docs/zh-TW/archive-strategy.md` 已定義 hot/archive sources、minimum payloads、retention classes、manifests、delete rules 與 restore rules；exporter jobs 與 restore smoke tests 仍是後續實作。

### 可觀測性

- [x] 補上 `/api/ops/metrics` baseline，涵蓋訂單狀態、下單延遲、撤單數與成交事件數。
- [ ] 補 metrics backend，以及撮合延遲、Kafka lag、DB latency、Redis latency、拒單率、成交率 collectors。
- [x] 補上 request id / correlation id header、MDC、outbox、Kafka、外部 API 傳遞 baseline。
- [ ] 補 distributed tracing export、dashboard 與 sampling policy。
- [x] 補 request/security audit structured logging baseline。
- [x] 補核心事件 structured logging，能按 uid、orderId、clientOrderId、symbol 搜尋。
  - Baseline 已完成：order lifecycle projection 會寫 `CORE_EVENT eventType=ORDER_LIFECYCLE` log line，包含穩定的 `uid`、`orderId`、`clientOrderId`、`symbol`、`stage`、`status`、`reasonCode`、`eventTs` 欄位。
- [ ] 建立 alert：撮合停止、Kafka lag、DLQ 堆積、對帳失敗、外部 API 錯誤率、資產不平。

## P2 可逐步演進

- [ ] Admin console：市場配置、風控參數、手動停牌、DLQ replay、對帳報表。
- [ ] 報表系統：用戶資產報表、交易報表、手續費報表、營運與財務日報。
- [ ] 壓測工具：下單 TPS、撮合 TPS、行情推送 fanout、Polymarket sync 壓力。
- [ ] 灰度與回滾：feature flag、canary deployment、schema backward compatibility。
- [ ] 合規能力：KYC/AML hook、制裁名單、交易監控、可疑行為報表。

## 近期落地順序建議

1. [x] 關閉 core-v1 freeze checklist。
2. [x] 執行 core-v1 smoke runbook。
3. [x] 只修 tests/checklists 發現的 release-blocking gaps。
4. [ ] tag 或 hand off 有邊界的 core-v1 baseline。
5. [x] v1 之後的工作重新規劃，不再塞進本次 freeze。
