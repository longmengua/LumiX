# P21-T06 Deterministic Replay、Resync 與 Recovery Boundary

```text
Task ID: P21-T06
Task name: Deterministic Replay、Resync 與 Recovery Boundary
Status: PROPOSED_AWAITING_HUMAN_REVIEW
Objective: 將 T03–T05 的 state transition 封裝成 deterministic replay 與不連線的 resync/recovery policy。
Why this task exists: 缺 gap 後 recovery 契約會讓系統在不完整資料下繼續輸出；使用 wall clock 或非固定集合順序則無法稽核重放。
Prerequisites: P21-T03, P21-T04 and P21-T05 implemented and human-reviewed；本 task card 已獲 implementation approval。
Dependencies: P21-T03, P21-T04, P21-T05。
Scope: ordered replay input、initial state、transition result digest、resync command/request model、recovery state machine、fixture corpus 與 deterministic verification。
Out of scope: provider reconnect/fetch、message broker、database event store、scheduler、production recovery automation、public stream。
Deliverables: pure replay coordinator、resync request contract、state digest/comparison、failure isolation policy 與 tests。
Allowed files: server/src/main/java/com/lumix/marketdata/**、server/src/test/java/com/lumix/marketdata/**、docs/phases/PHASE_21_MARKET_DATA/。
Forbidden files: external provider/client SDK、network I/O、migration、web/src/、matching/ledger/balance/position/reservation/settlement code。
Domain model: replay input batch、immutable initial state、stream transition trace、projection/ticker/candle/health digest、resync request/reason、recovery boundary。
Core invariants: 相同 initial state 加相同 canonical event order 必得相同 projections/health/digest；gap 後只接受相容 resync snapshot；resync 不產生交易或修正既有資料。
Input contract: explicit event list、explicit initial state、explicit evaluation time/configuration；集合必須先以 stream key/sequence canonicalize 或拒絕歧義順序。
Output contract: immutable final state、transition trace、digest、pending resync request；無 network side effect。
Failure behavior: gap/schema mismatch/crossed book/overflow/ambiguous order 回傳 failed/degraded trace；不得吞錯或用現在時間改寫結果。
Idempotency / duplicate behavior: replay 使用 T03 duplicate policy；重放同一 input 不增加 state 或 resync request。
Sequence behavior: 各 stream 依 canonical sequence 重放；gap 產生 resync-required 並隔離該 stream，其他 instrument 可獨立重放。
Time semantics: replay evaluation time 由 input 指定；禁止 `Instant.now()`、sleep、random；保留 source/received/processed time 的既定語意。
Precision rules: 只使用 T02/T04/T05 精度規則；digest 的數值 serialization 必須 canonical plain string，不可用 scientific/locale-dependent format。
Concurrency assumptions: coordinator 可 pure single-thread；若未來平行，合併結果必須依 stream key 的固定排序，不能改 digest。
Persistence impact: none；event list 是 fixture/in-memory input，不宣稱 event store。
Schema changed: no。
Money-impacting: no。
HUMAN_REVIEW_REQUIRED: yes；recovery policy 決定資料不完整時的 fail-closed 行為。
Security considerations: 限制 replay batch/state size，避免不可信輸入耗盡資源；trace 不得含 credential/payload secret。
Observability requirements: replay digest、transition failure reason、resync count/age、isolated stream、determinism mismatch evidence。
Tests required: repeat replay equality、輸入重排的 canonical/拒絕行為、gap->resync->snapshot recovery、multi-instrument isolation、clock independence、極端數值。
Acceptance criteria: 無 wall clock/random/network I/O；gap 不會被自動略過；每個 resync 有可追溯 reason。
Rollback notes: revert pure coordinator；無持久資料或外部連線需撤回。
Stop conditions: 需要實際 reconnect/snapshot pull、持久化 offset 或自動操作 provider 時停止，另建 provider-specific card。
Documentation updates: deterministic replay evidence、resync/recovery boundary、task status。
Next task: P21-T07。
```
