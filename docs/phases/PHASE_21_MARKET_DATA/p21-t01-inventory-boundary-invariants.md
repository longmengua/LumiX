# P21-T01 現況盤點、領域邊界與核心不變式

```text
Task ID: P21-T01
Task name: 現況盤點、領域邊界與核心不變式
Status: IMPLEMENTATION_REVIEW_APPROVED
Objective: 將既有 market stub、sandbox order book 與前端 mock 的責任分離，建立後續實作必須遵守的唯讀行情邊界。
Why this task exists: 現有展示 DTO、Phase 10 空 stub、sandbox matching state 與 UI fixture 的名稱相近；未先固定邊界容易把 mock 或交易狀態誤接為行情真相。
Prerequisites: Phase 20 review completed；本 task card 已獲人類 implementation approval。
Dependencies: none。
Scope: 盤點相關 package、補齊 Phase 21 architecture decision / invariant note、定義 package 命名與 owner；不改 runtime。
Out of scope: event runtime、provider connection、API、WebSocket、schema、任何 projection 實作。
Deliverables: 現況/缺口表、唯讀責任圖、核心不變式、允許的後續 package map 與明確 no-claim 文字。
Allowed files: docs/phases/PHASE_21_MARKET_DATA/、AI_PROGRESS.md、AGENTS.md、AI_AGENT.md、docs/phases/README.md、docs/ai/AI_CONTEXT_ROUTING.md。
Forbidden files: server/src/、server migration、web/src/、build dependency、production configuration、CI/CD。
Domain model: 僅文件模型：行情來源、normalized event、projection、aggregation、health、consumer；既有 `com.lumix.market` 只視為 legacy stub boundary。
Core invariants: 行情資料唯讀；不使用 sandbox order book、trade/fill、mock UI 或本機 clock 作為 authoritative source；不可連至交易/資金 mutation。
Input contract: repository 在 approved baseline 上的唯讀 inventory。
Output contract: 可供 T02 使用的 package/責任表，並列出所有禁止依賴。
Failure behavior: 發現既有耦合、浮點數或未定義時間語意時，記錄 gap 並停止於文件，禁止用 code workaround 掩蓋。
Idempotency / duplicate behavior: 文件盤點可重複執行；相同 baseline 產生相同結論，差異須記錄 commit。
Sequence behavior: 不建立 runtime sequence；定義 T02/T03 的 sequence owner 與比較 scope。
Time semantics: 盤點須區分 mock `Date.now()`、stub `Instant.now()` 與未來 source/received/processed timestamps，均不可宣稱事件 identity。
Precision rules: 記錄現有 `MoneyAmount(BigDecimal)` 無 instrument scale/rounding/overflow contract，以及 web mock `number` 不得沿用。
Concurrency assumptions: 無 runtime；後續 projection 須以 instrument-stream key 隔離，不以全域共享可變 state 起步。
Persistence impact: none。
Schema changed: no。
Money-impacting: no。
HUMAN_REVIEW_REQUIRED: yes；需審查此邊界不會把行情導向 matching、position、balance、ledger 或 settlement。
Security considerations: 不請求 API key、secret 或 production endpoint；文件不可將 provider 當成已授權。
Observability requirements: 列出未來需要的 source/channel/instrument、sequence、lag、health、resync evidence 欄位。
Tests required: 文件一致性檢查；確認無 runtime changed、無過期 Phase 12 warning、無 production claim。
Acceptance criteria: inventory 明確標示 stub、sandbox 與 mock；每個後續 task 有唯一責任；禁止邊界可由 review 檢查。
Rollback notes: 純文件變更可用 revert commit 移除；不得改寫既有 review history。
Stop conditions: 任何需求涉及 runtime、外部 provider、公開 stream、schema 或交易/資金狀態即停止並建立新 task card。
Documentation updates: 已更新本 phase README、task status、規劃/執行 review note、AI_PROGRESS.md 與 agent routing 狀態；盤點與不變式詳見 `p21-t01-implementation-review.md`。
Next task: P21-T02；implementation review 已通過，且本 task 已獲人類明確實作批准。P21-T03 仍須等待其 task card 的人類批准。
```
