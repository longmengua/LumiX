# P21-T04 唯讀 Order Book Snapshot / Delta Projection

```text
Task ID: P21-T04
Task name: 唯讀 Order Book Snapshot / Delta Projection
Status: PROPOSED_AWAITING_HUMAN_REVIEW
Objective: 以已驗證的 snapshot/delta 事件建立 immutable、唯讀的 order-book projection。
Why this task exists: 既有 spot sandbox order book 保存交易 sandbox order，UI mock book 是合成版面資料；兩者都不是外部行情的 authoritative projection。
Prerequisites: P21-T02 and P21-T03 implemented and human-reviewed；本 task card 已獲 implementation approval。
Dependencies: P21-T02, P21-T03。
Scope: book snapshot/delta payload、price-level aggregation、book status、crossed/empty detection、immutable query snapshot 與 contract tests。
Out of scope: matching、internal order intake、trade/fill producer、持久化、公開 depth API/WebSocket、provider snapshot client。
Deliverables: read-only projection reducer、snapshot/delta compatibility rule、book availability/status contract、fixture-based tests。
Allowed files: server/src/main/java/com/lumix/marketdata/**、server/src/test/java/com/lumix/marketdata/**、docs/phases/PHASE_21_MARKET_DATA/。
Forbidden files: `server/src/main/java/com/lumix/trading/core/spot/orderbook/**`、matching、ledger、balance、position、reservation、settlement、migration、web/src/、external provider code。
Domain model: instrument stream key、book side、price level、snapshot sequence、delta sequence、projection sequence、`UNAVAILABLE|SYNCING|HEALTHY|STALE|GAP_DETECTED|RESYNC_REQUIRED|DEGRADED`。
Core invariants: delta 無相容 snapshot 時拒絕；僅 T03 accepted continuous delta 可套用；bid 由高至低、ask 由低至高；quantity 為零表示移除；crossed book 不可標示 healthy/authoritative。
Input contract: T02 book snapshot/delta payload 加 T03 admission decision；snapshot 含完整性/sequence 範圍，delta 指向連續序列。
Output contract: immutable read-only projection、as-of source sequence/time、health/status 與拒絕原因；不公開為 production API。
Failure behavior: 缺 snapshot、gap、錯誤 instrument、負數/超 scale quantity、crossed book、未知 side 一律不套用並將 projection 降級或維持 unavailable。
Idempotency / duplicate behavior: T03 duplicate decision 不改 projection；同一 accepted delta 不可套用兩次。
Sequence behavior: snapshot 成為 baseline；delta 必須嚴格連續且與 snapshot stream 相符；gap 後一律等待 resync snapshot。
Time semantics: projection as-of 使用最後 accepted event 的 sourceTimestamp；received/processed timestamps 只作診斷；不自行讀 clock。
Precision rules: price/quantity 使用 T02 fixed-scale/atomic value object；比較與排序不得轉 double；累加必須做 overflow check 並禁止隱式 rounding。
Concurrency assumptions: 每個 book stream 單一序列 reducer；對外僅發布 immutable copy，讀者不能改內部 state。
Persistence impact: none；不可寫 DB/cache。
Schema changed: no。
Money-impacting: no。
HUMAN_REVIEW_REQUIRED: yes；需確認 projection 不被誤接成 matching order book 或正式流動性聲稱。
Security considerations: 限制 level 數與 payload size；拒絕畸形大量 delta，避免 memory/CPU exhaustion。
Observability requirements: snapshot accepted、delta applied/rejected、book status、crossed count、level count、last sequence/lag、resync-needed reason。
Tests required: 空 book、snapshot 後 delta、缺 snapshot delta、duplicate、gap、out-of-order、crossed book、極端數值、multi-instrument isolation、deterministic replay。
Acceptance criteria: 不足資料永不形成 authoritative book；每個 fixture 重放得到相同 level ordering/status；無交易核心依賴。
Rollback notes: revert projection package；無外部 state 或 schema。
Stop conditions: 任何需求要從 internal orders、matching output、UI mock 或 provider connection 建 book 時停止並升級審核。
Documentation updates: snapshot/delta contract、book status meaning、task status。
Next task: P21-T06；P21-T07 需同時等待 T05 與 T06。
```
