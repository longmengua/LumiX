<!-- 檔案用途：整理正式環境上線前建議補齊的功能、可靠性、安全與維運工作。 -->
# Production TODO

這份清單聚焦「要把目前 MVP 推向 production」前應補齊的能力。優先順序可依產品階段調整，但 P0/P1 建議在真實資金或真實交易量進來前完成。

## P0 必做

### 交易與撮合

- 將 in-memory matching engine 演進為可 replay 的撮合核心，至少要有 command log、event log、snapshot、offset checkpoint。
- 定義單 symbol sequencer 的部署與 failover 策略，避免多實例同時處理同一 symbol 造成狀態分裂。
- 補齊 order lifecycle event：created、accepted、rejected、partially filled、filled、canceled、expired。
- 補齊 amend order、cancel replace、bulk cancel、cancel on disconnect 等交易所常見指令。
- 嚴格落實 tick size、lot size、min notional、price band、max order size、max open orders。
- 明確處理 MARKET 流動性不足、IOC/FOK 未完全成交、POST_ONLY 會吃單、REDUCE_ONLY 超過可減倉量等拒單原因。

### 帳務與資金

- 建立完整雙分錄 ledger schema，所有資金變動必須可追溯、可重放、可對帳。
- 將 order reserve、position margin、fee、rebate、realized PnL、funding、liquidation loss 拆成明確 accounting entries。
- 補齊帳戶資產凍結、解凍、可用餘額、總權益、維持保證金、風險率的不可變計算規則。
- 建立日終對帳與即時對帳任務，檢查 account、position、ledger、event store 是否一致。
- 補齊入金/出金狀態機：pending、confirmed、failed、reversed、manual review。

### 風控

- 接入 mark price / index price oracle，避免 liquidation 與 funding 使用成交價或任意輸入價。
- 建立 symbol risk tier：最大槓桿、維持保證金率、初始保證金率、階梯倉位上限。
- 補齊 pre-trade risk check：餘額、槓桿、倉位、敞口、價格偏離、頻率、client order id 去重。
- 補齊 liquidation engine：掃描、觸發、成交、保險基金、ADL、事件審計。
- 加上全站風控開關：只減倉、禁止下單、禁止提現、指定 symbol 停牌。

### 可靠性與一致性

- Outbox 需要有持久化儲存、retry backoff、最大重試、DLQ replay、人工補償流程。
- Kafka topic 需要規劃 partition key、retention、compaction、schema version 與 consumer group 策略。
- 所有外部 API 呼叫需要 timeout、retry、circuit breaker、rate limit 與 idempotency key。
- 所有核心寫入需要明確交易邊界；MySQL、Redis、Kafka 之間不能假設天然一致。
- 建立災難恢復流程：從 snapshot + event log 恢復 matching/order/account/position。

### 安全

- API authentication / authorization：JWT、API key、scope、角色權限、admin 權限隔離。
- 將 private key、CLOB secret、relayer key 移出 YAML，改由 secret manager 或環境變數注入。
- Session signer lifecycle 要有過期、撤銷、審計、異常使用檢測。
- 所有交易、資金、admin API 加上 rate limit、IP allowlist、審計日誌。
- 敏感欄位禁止進 log：private key、api secret、passphrase、signature、authorization header。

## P1 強烈建議

### Market Data

- 建立增量 order book stream，支援 sequence number、checksum、snapshot + delta 重建。
- 將 ticker、kline、trade tape 持久化，避免服務重啟後行情資料消失。
- WebSocket/SSE gateway 獨立部署，支援水平擴展、訂閱權限、心跳、限流、斷線補償。
- 補齊 market maker / liquidity provider 專用 API 與節流策略。

### Polymarket 整合

- 建立 Polymarket order 狀態機，完整追蹤 local order、CLOB order、trade、settlement lifecycle。
- 將 Gamma/CLOB response schema version 化，避免遠端欄位變更造成解析錯誤。
- 對 CLOB 下單、取消、同步、reconcile 做 idempotent command 設計。
- User WebSocket 服務獨立部署，支援自動重連、checkpoint、事件去重、落庫與 replay。
- allowance / approval 查詢加入 cache 與過期策略，避免 RPC 被打爆。

### 資料庫與儲存

- 為 orders、positions、ledger、events、prediction orders 補齊 production index。
- 將 Redis key schema 文件化，補 TTL、namespace、版本與 migration 策略。
- Flyway migration 改為正式唯一 schema 管理，不再依賴 Hibernate `ddl-auto=update`。
- 補齊資料歸檔策略：歷史訂單、成交、ledger、Kafka event、audit log。

### 可觀測性

- 加入 metrics：下單延遲、撮合延遲、Kafka lag、DB latency、Redis latency、拒單率、成交率。
- 加入 tracing：request id / correlation id 貫穿 API、UseCase、Kafka、外部 API。
- 加入 structured logging，核心事件要能按 uid、orderId、clientOrderId、symbol 搜尋。
- 建立 alert：撮合停止、Kafka lag、DLQ 堆積、對帳失敗、外部 API 錯誤率、資產不平。

## P2 可逐步演進

- Admin console：市場配置、風控參數、手動停牌、DLQ replay、對帳報表。
- 報表系統：用戶資產報表、交易報表、手續費報表、營運與財務日報。
- 壓測工具：下單 TPS、撮合 TPS、行情推送 fanout、Polymarket sync 壓力。
- 灰度與回滾：feature flag、canary deployment、schema backward compatibility。
- 合規能力：KYC/AML hook、制裁名單、交易監控、可疑行為報表。

## 近期落地順序建議

1. 先補 order lifecycle event 與持久化 order/ledger/event schema。
2. 再把 matching engine 加上 command log、snapshot、replay。
3. 接 mark price / index price，完成 production 級 pre-trade risk 與 liquidation。
4. 建立 reconciliation job 與 observability baseline。
5. 最後拆分 WebSocket gateway、Polymarket WS worker、matching worker。
