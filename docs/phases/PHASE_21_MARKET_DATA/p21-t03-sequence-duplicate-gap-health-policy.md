# P21-T03 Sequence、Duplicate、Gap 與 Feed Health Policy

```text
Task ID: P21-T03
Task name: Sequence、Duplicate、Gap 與 Feed Health Policy
Status: PROPOSED_AWAITING_HUMAN_REVIEW
Objective: 對每條 source/channel/instrument stream 定義可重放的 admission decision 與 health state machine。
Why this task exists: 無 sequence policy 時 duplicate、亂序與 gap 會被靜默混入 projection，讓下游誤信不完整資料。
Prerequisites: P21-T02 implemented and human-reviewed；本 task card 已獲 implementation approval。
Dependencies: P21-T02。
Scope: sequence cursor、duplicate fingerprint、out-of-order/gap decision、feed health state machine、stale threshold 的注入式 policy、decision record 與測試 fixture。
Out of scope: order-book/ticker/candle projection、provider reconnect、snapshot fetch、persistence、external scheduler、public stream。
Deliverables: immutable decision model、per-stream policy service、health transitions、固定 reason code 與 unit tests。
Allowed files: server/src/main/java/com/lumix/marketdata/**、server/src/test/java/com/lumix/marketdata/**、docs/phases/PHASE_21_MARKET_DATA/。
Forbidden files: server migration、web/src/、external provider client、`trading/core/**`、ledger/balance/position/reservation/settlement package。
Domain model: stream key、last accepted sequence、last source/received timestamp、event fingerprint、`HEALTHY|STALE|GAP_DETECTED|RESYNC_REQUIRED|DEGRADED|STOPPED`、admission decision。
Core invariants: duplicate 不重複套用；out-of-order 不套用；gap 立即停止該 stream 的 delta admission 並進入 `GAP_DETECTED`/`RESYNC_REQUIRED`；非 healthy 不可被標記為即時正常。
Input contract: T02 normalized event、該 stream 的前一 immutable cursor、注入的 evaluation timestamp 與 stale policy。
Output contract: accepted/duplicate/rejected/resync-required decision、下一 cursor 與 health state；禁止直接改 projection。
Failure behavior: sequence 跳躍、payload identity 衝突、未知 stream base state 或 stale timeout 均產生可觀測拒絕/降級；不靜默補號。
Idempotency / duplicate behavior: 同 identity/fingerprint event 回傳 duplicate no-op；同 sequence 不同 payload 視為 integrity conflict 並 `DEGRADED`。
Sequence behavior: snapshot 建立 baseline，delta 必須為 expected next sequence；較小 sequence 是 out-of-order，較大 sequence 是 gap；跨 stream 完全隔離。
Time semantics: stale 僅比較注入的 evaluation timestamp 與 receivedTimestamp；sourceTimestamp 可用於遲到事件診斷，不以 wall clock 改變 replay。
Precision rules: 不重算數值；保留 T02 的 scale/value，不做 rounding 或 double conversion。
Concurrency assumptions: 同一 stream 的 cursor transition 必須序列化；不同 stream 可獨立處理；並行策略不得改變 decision ordering。
Persistence impact: in-memory policy foundation only；不得引入 schema 或 durable offset。
Schema changed: no。
Money-impacting: no。
HUMAN_REVIEW_REQUIRED: yes；需審查 stale/gap 判定是否 fail closed 且不誤導下游。
Security considerations: 限制 stream key cardinality 與拒絕診斷內容，避免不受控 identifier 造成資源耗盡或資料洩漏。
Observability requirements: accepted/duplicate/out-of-order/gap counts、stream health、sequence lag、received lag、resync requested reason。
Tests required: 正常序列、duplicate、同 sequence 異 payload、out-of-order、gap、stale、STOPPED、multi-instrument isolation、deterministic decision replay。
Acceptance criteria: 任一 gap 不會被接受為 healthy delta；所有 transition 可用固定 fixture 重放；沒有投影或外部 I/O。
Rollback notes: 可 revert 新 policy package；無資料/Schema 需回滾。
Stop conditions: 需要 snapshot 下載、provider reconnect、timer thread 或 consumer endpoint 時停止，交由 T06/T07 或新 provider card。
Documentation updates: health transition table、resync trigger 與 task status。
Next task: P21-T04 與 P21-T05 可在本卡核准完成後分別施工。
```
