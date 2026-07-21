# P21-T07 唯讀內部 Query / Stream Contract 與 Backpressure Policy

```text
Task ID: P21-T07
Task name: 唯讀內部 Query / Stream Contract 與 Backpressure Policy
Status: PROPOSED_AWAITING_HUMAN_REVIEW
Objective: 將健康標記的 projection 以唯讀內部 query/stream contract 交給未來 consumer，並定義無損害資料正確性的 backpressure 行為。
Why this task exists: 現有 OpenAPI 僅是 route metadata，沒有實際 API；若直接接公開 WebSocket，可能把 stale 或不完整資料誤包裝成即時服務。
Prerequisites: P21-T04, P21-T05 and P21-T06 implemented and human-reviewed；本 task card 已獲 implementation approval。
Dependencies: P21-T04, P21-T05, P21-T06。
Scope: internal read-only query interface、snapshot subscription message contract、health/sequence/as-of metadata、consumer lag/backpressure policy、in-memory contract tests。
Out of scope: HTTP controller、公開/private WebSocket server、authentication、rate limiter、broker、UI migration、provider connection、任何 command/mutation endpoint。
Deliverables: internal port、immutable view envelope、subscription/backpressure policy、consumer isolation tests 與 no-public-runtime guard。
Allowed files: server/src/main/java/com/lumix/marketdata/**、server/src/test/java/com/lumix/marketdata/**、docs/phases/PHASE_21_MARKET_DATA/。
Forbidden files: server web/controller/websocket packages、OpenAPI route implementation、web/src/、security/auth、provider client、migration、trading/ledger/balance/position/reservation/settlement。
Domain model: query result envelope、stream update envelope、projection version/sequence、health/as-of metadata、subscriber cursor、`DROP_AND_RESNAPSHOT|DISCONNECT_AND_RESNAPSHOT` backpressure outcome。
Core invariants: 只讀；所有輸出攜帶 health 與 as-of sequence/time；consumer 落後時不得悄悄跳過 delta 後仍聲稱連續；consumer 不得取得可變 reducer state。
Input contract: 已完成 T04/T05/T06 的 immutable projection updates 與明確 subscriber capacity/cursor。
Output contract: immutable current view 或 ordered update envelope；非 healthy 狀態可見；backpressure 必須帶 resnapshot/disconnect reason。
Failure behavior: 未知 instrument、過期 cursor、health 非 healthy、consumer overflow、version mismatch 均 fail closed 或要求 resnapshot；不回傳假 live data。
Idempotency / duplicate behavior: query 可重複；subscription duplicate version 不重複發布；resnapshot 是完整 immutable replacement，不補造 delta。
Sequence behavior: envelope 保留 projection sequence；consumer 遇缺號或 overflow 必須丟棄其局部 cursor 並 resnapshot/disconnect。
Time semantics: transport 不生成 market event time；原樣傳遞 source/received/processed/as-of metadata；backpressure timeout 必須為顯式注入 policy。
Precision rules: 直接傳遞 canonical decimal/atomic string；不得在 transport 轉為 JSON number/double 或格式化/rounding。
Concurrency assumptions: reducer 與 consumer 隔離；每個 subscriber 自有 cursor/capacity；慢 consumer 不阻塞其他 instrument 或 reducer。
Persistence impact: no persistence；不得寫 subscription、offset 或 audit schema。
Schema changed: no。
Money-impacting: no。
HUMAN_REVIEW_REQUIRED: yes；需確認 stale/degraded 與 loss/backpressure 不能被 consumer 誤解為即時完整行情。
Security considerations: 內部 port 不接受網路暴露；限制 subscriber/queue 大小，避免 resource exhaustion；不夾帶 secret。
Observability requirements: query status、subscriber count、consumer lag、drop/disconnect/resnapshot count、health distribution、last published sequence。
Tests required: healthy/degraded query、stale visible、consumer overflow、gap cursor、resnapshot、multi-instrument isolation、immutable envelope、decimal serialization。
Acceptance criteria: 無 controller/WebSocket runtime；慢 consumer 不改 projection 且不能無聲遺失後繼續；所有 view 有 health metadata。
Rollback notes: revert internal port；無外部 contract、schema 或 consumer migration。
Stop conditions: 需要公開 endpoint、認證、production WebSocket、UI replacement 或 broker 時停止並建立獨立批准 task。
Documentation updates: internal-only contract、backpressure matrix、task status。
Next task: P21-T08。
```
