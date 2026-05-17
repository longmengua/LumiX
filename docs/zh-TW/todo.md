<!-- 檔案用途：繁體中文 production TODO；其他語言請見根目錄 README.md。 -->
# Production TODO

這份清單聚焦「要把目前 MVP 推向 production」前應補齊的能力。優先順序可依產品階段調整，但 P0/P1 建議在真實資金或真實交易量進來前完成。

文件分類：[產品文件](README.md) / [技術文件](technical.md) / 待辦清單文件

## P0 必做

### 交易與撮合

- [ ] 將 in-memory matching engine 演進為可 replay 的撮合核心，至少要有 command log、event log、snapshot、offset checkpoint。
- [x] 補上 in-process 單 symbol sequencer baseline，讓同一 symbol 的撮合操作序列化。
- [ ] 定義 production 單 symbol sequencer 的部署與 failover 策略，避免多實例同時處理同一 symbol 造成狀態分裂。
- [x] 發布 order lifecycle events，涵蓋 created、accepted、updated、rejected、canceled、expired、filled。
- [ ] 將 order lifecycle events 產品化，補 durable storage、schema version、replay 與查詢 projection。
- [x] 補上 amend order、cancel replace、bulk cancel、cancel on disconnect 的 REST/WebSocket baseline。
- [ ] 為交易所常見指令補 durable command log、更強 atomicity mode 與 reconnect/session 語意。
- [x] 在 pre-trade checks 落實 tick size、lot size、min notional、price band、max order size、max open orders。
- [x] 明確處理 MARKET 流動性不足、IOC/FOK 未完全成交、POST_ONLY 會吃單、REDUCE_ONLY 超過可減倉量等拒單原因。

### 帳務與資金

- [x] 建立 wallet ledger balanced posting baseline，讓 MVP 資金變動可追溯、可對帳。
- [ ] 建立完整 production 雙分錄 ledger schema 與 replay path。
- [x] 將 order reserve、position margin、fee、rebate、realized PnL、funding、liquidation shortfall、deposit、withdrawal 拆成明確 accounting entries。
- [ ] 用 production database constraints、audit retention 與 replay validation 強化 accounting entries。
- [x] 補上 `/api/margin/risk`，計算凍結資金、可用餘額、總權益、維持保證金與風險率 snapshot。
- [ ] 持久化每日 account risk snapshots，並用獨立 mark/index oracle inputs 取代 trade/ticker fallback marks。
- [x] 補上全帳戶對帳 baseline，掃 maintained account index 與 open-position index，回報 account、position margin、ledger balance 問題。
- [ ] 補 persisted reconciliation reports、排程策略、alert routing 與 event-store coverage。
- [x] 補上 Redis-backed 入金/出金狀態機 baseline，支援 pending、confirmed、failed、reversed、manual review transfer states。
- [ ] 補鏈上/銀行 callbacks、人工覆核流程 owner 與 transfer reconciliation projections。

### 風控

- [ ] 接入 mark price / index price oracle，避免 liquidation 與 funding 使用成交價或任意輸入價。
- [x] 建立 symbol risk baseline 設定：最大槓桿、維持保證金率、最大倉位名義金額、最大 open orders。
- [ ] 補完整 risk tier：初始保證金率與階梯倉位上限。
- [x] 補上 pre-trade risk checks：餘額、槓桿、倉位、敞口、價格偏離、client order id 去重。
- [ ] 為 pre-trade risk checks 補 production frequency limits 與更完整 abuse controls。
- [x] 補上 liquidation MVP，涵蓋觸發、平倉、保險基金、ADL 與 audit event。
- [ ] 補 production liquidation scanning、execution routing 與 operational controls。
- [x] 加上全站風控開關：只減倉、禁止下單、禁止提現、指定 symbol 停牌。

### 可靠性與一致性

- [x] 補上 outbox retry backoff、最大重試、DLQ replay 與人工補償流程 baseline。
- [ ] 將 outbox 移到 production durable storage，並補人工補償 runbook。
- [x] 文件化 Kafka topic partition key、retention、compaction、schema version 與 consumer group 策略。
- [x] 補上外部 API 共用 HTTP timeout、retry、circuit breaker 與 rate-limit baseline。
- [ ] 逐一確認所有外部 API 都具備 timeout、retry、circuit breaker、rate limit 與 idempotency coverage。
- [ ] 所有核心寫入需要明確交易邊界；MySQL、Redis、Kafka 之間不能假設天然一致。
- [x] 補上 MVP snapshot + event replay recovery 入口。
- [ ] 建立 production 災難恢復流程：從 snapshot + event log 恢復 matching/order/account/position。

### 安全

- [ ] Session signer lifecycle 要有過期、撤銷、審計、異常使用檢測。

## P1 強烈建議

### Market Data

- [x] 補上 REST/SSE depth delta，帶 monotonic version 與 CRC32 checksum，支援 snapshot + delta 校驗。
- [ ] 補 durable sequence checkpoint 與 reconnect backfill。
- [ ] 將 ticker、kline、trade tape 持久化，避免服務重啟後行情資料消失。
- [ ] WebSocket/SSE gateway 獨立部署，支援水平擴展、訂閱權限、心跳、限流、斷線補償。
- [ ] 補齊 market maker / liquidity provider 專用 API 與節流策略。

### Polymarket 整合

- [ ] 建立 Polymarket order 狀態機，完整追蹤 local order、CLOB order、trade、settlement lifecycle。
- [ ] 將 Gamma/CLOB response schema version 化，避免遠端欄位變更造成解析錯誤。
- [ ] 對 CLOB 下單、取消、同步、reconcile 做 idempotent command 設計。
- [ ] User WebSocket 服務獨立部署，支援自動重連、checkpoint、事件去重、落庫與 replay。
- [ ] allowance / approval 查詢加入 cache 與過期策略，避免 RPC 被打爆。

### 資料庫與儲存

- [ ] 為 orders、positions、ledger、events、prediction orders 補齊 production index。
- [x] 文件化 Redis key schema、namespace prefix、版本與 migration 策略。
- [ ] 補 Redis hot-state key 的最終 TTL / archive rules。
- [ ] Flyway migration 改為正式唯一 schema 管理，不再依賴 Hibernate `ddl-auto=update`。
- [ ] 補齊資料歸檔策略：歷史訂單、成交、ledger、Kafka event、audit log。

### 可觀測性

- [x] 補上 `/api/ops/metrics` baseline，涵蓋訂單狀態、下單延遲、撤單數與成交事件數。
- [ ] 補 metrics backend，以及撮合延遲、Kafka lag、DB latency、Redis latency、拒單率、成交率 collectors。
- [x] 補上 request id / correlation id header、MDC、outbox、Kafka、外部 API 傳遞 baseline。
- [ ] 補 distributed tracing export、dashboard 與 sampling policy。
- [x] 補 request/security audit structured logging baseline。
- [ ] 補核心事件 structured logging，能按 uid、orderId、clientOrderId、symbol 搜尋。
- [ ] 建立 alert：撮合停止、Kafka lag、DLQ 堆積、對帳失敗、外部 API 錯誤率、資產不平。

## P2 可逐步演進

- [ ] Admin console：市場配置、風控參數、手動停牌、DLQ replay、對帳報表。
- [ ] 報表系統：用戶資產報表、交易報表、手續費報表、營運與財務日報。
- [ ] 壓測工具：下單 TPS、撮合 TPS、行情推送 fanout、Polymarket sync 壓力。
- [ ] 灰度與回滾：feature flag、canary deployment、schema backward compatibility。
- [ ] 合規能力：KYC/AML hook、制裁名單、交易監控、可疑行為報表。

## 近期落地順序建議

1. [ ] 先補 order/ledger/event 持久化 schema 與 order lifecycle projection。
2. [ ] 再把 matching engine 加上 command log、snapshot、replay。
3. [ ] 接 mark price / index price，完成 production 級 pre-trade risk 與 liquidation。
4. [x] 建立 MVP reconciliation job 與 observability baseline。
5. [ ] 最後拆分 WebSocket gateway、Polymarket WS worker、matching worker。
