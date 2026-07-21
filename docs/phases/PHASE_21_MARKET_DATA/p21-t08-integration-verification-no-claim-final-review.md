# P21-T08 Integration Verification、No-Claim Gate 與 Phase Final Review

```text
Task ID: P21-T08
Task name: Integration Verification、No-Claim Gate 與 Phase Final Review
Status: PROPOSED_AWAITING_HUMAN_REVIEW
Objective: 驗證 P21 foundation 的跨 task 不變式、no-claim 邊界與 review evidence；產出供人類判斷的 phase final review，不授權新 runtime。
Why this task exists: 單元測試通過不代表 aggregate boundary、health 表達或 production claim 一致；需要一張獨立 gate 防止把 foundation 誤宣稱為正式行情服務。
Prerequisites: P21-T01 through P21-T07 implemented, reviewed and approved individually；本 task card 已獲 implementation approval。
Dependencies: P21-T01, P21-T02, P21-T03, P21-T04, P21-T05, P21-T06, P21-T07。
Scope: integration fixtures、architecture/no-claim tests、review evidence、phase final review draft、rollback/revert instructions。
Out of scope: 新增行情功能、外部 provider、API/WebSocket、schema/persistence、matching/trade/fill producer、任何資金或交易 mutation。
Deliverables: integration verification report、test matrix、architecture guard、phase final review draft 與明確仍未完成項目。
Allowed files: server/src/test/java/com/lumix/marketdata/**、docs/phases/PHASE_21_MARKET_DATA/、AI_PROGRESS.md、AGENTS.md、AI_AGENT.md、docs/phases/README.md、docs/ai/AI_CONTEXT_ROUTING.md。
Forbidden files: server/src/main runtime feature code、migration、web/src/、production config、CI/CD、external provider integration、交易與資金 package。
Domain model: 跨 reducer replay fixture、review evidence、no-claim assertion；不新增 production domain state。
Core invariants: integration 不得跨入 matching、ledger、position、balance、reservation、settlement、wallet；stale/gap/resync 必須在所有 consumer view 中可見；無 task 可把 mock 當 production capability。
Input contract: 所有已批准 P21 card 的 immutable contracts、fixtures 與 test results。
Output contract: 可重現 verification evidence 與 final-review draft；不改 runtime state。
Failure behavior: 任一 invariant/architecture/no-claim test 失敗即不產出 completed/approved claim，回到對應 task 修正並重新人審。
Idempotency / duplicate behavior: 相同 approved artifact 和 fixture 得到相同 review evidence；重跑不得產生 runtime side effect。
Sequence behavior: 驗證正常、duplicate、out-of-order、gap、snapshot/delta/resync 的 end-to-end transition，確認不跳過 sequence。
Time semantics: fixture 固定三種 timestamp；驗證不使用 wall clock 導致結果差異。
Precision rules: 驗證所有 domain/transport fixture 無 float/double，decimal serialization、rounding rejection 與 overflow 行為一致。
Concurrency assumptions: integration tests 必須證明 stream/instrument isolation；不引入併發 runtime。
Persistence impact: none。
Schema changed: no。
Money-impacting: no。
HUMAN_REVIEW_REQUIRED: yes；最終檢查需由人類判定 foundation 是否符合 task cards，且不得推論 production readiness。
Security considerations: 確認無 secret、provider endpoint、公開 transport 或安全 bypass 混入 phase。
Observability requirements: review report 收錄 metrics contract、health/resync evidence 與未實作 operational monitoring 限制。
Tests required: 完整測試矩陣：正常、duplicate、out-of-order、gap、snapshot/delta、缺 snapshot、stale、resync、precision、極端值、空/crossed book、replay、multi-instrument、錯 instrument、schema mismatch、backpressure。
Acceptance criteria: final review 清楚記載測試與 no-claim；所有 task 狀態與入口一致；沒有任何 task 自行標示 approved/completed。
Rollback notes: 文件與測試可用 revert commit 回復；不得改寫既有 review 審計歷史。
Stop conditions: 發現 runtime 需求、外部連線、production claim 或尚未批准的前置 task 時停止並要求人類決策。
Documentation updates: phase final review、README、AI progress/routing/status only if human signs off later。
Next task: none；等待人類 phase review 與後續獨立 provider/public delivery task cards。
```
