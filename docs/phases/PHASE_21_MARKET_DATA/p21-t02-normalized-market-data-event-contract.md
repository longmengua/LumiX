# P21-T02 Normalized Market Data Event Contract

```text
Task ID: P21-T02
Task name: Normalized Market Data Event Contract
Status: COMPLETED_AWAITING_IMPLEMENTATION_REVIEW
Objective: 建立 provider-neutral、immutable 的 normalized market-data event 與明確精度/serialization 契約。
Why this task exists: 現有 `com.lumix.market` DTO 只有展示欄位，缺少來源、channel、sequence、schema version、三種時間與可驗證 payload identity。
Prerequisites: P21-T01 implemented and human-reviewed；本 task card 已獲 implementation approval。
Dependencies: P21-T01。
Scope: 在新的唯讀 market-data package 建立 event envelope、event type、instrument identifier、source/channel identifier、decimal/atomic quantity value object、schema-version validation 與 fixture contract。
Out of scope: 外部 provider client、API/WebSocket handler、database、cache、event broker、producer、交易/帳本/部位 mutation。
Deliverables: immutable Java domain contract、fixture builders、serialization mapping specification 與 contract tests。
Allowed files: server/src/main/java/com/lumix/marketdata/**、server/src/test/java/com/lumix/marketdata/**、docs/phases/PHASE_21_MARKET_DATA/。
Forbidden files: server migration、既有 `trading/core/**` sandbox package、ledger/balance/reservation/settlement package、web/src/、production configuration、external provider dependency。
Domain model: `NormalizedMarketDataEvent { source, channel, instrumentId, eventType, sequence, sourceTimestamp, receivedTimestamp, schemaVersion, payload }`；payload 為 book snapshot/delta、trade 或 ticker input 的 sealed/read-only variants。
Core invariants: identity 不是本機時間；source/channel/instrument stream key 必須完整；price/quantity/volume 不用 float/double；未知 schema version 一律拒絕。
Input contract: 呼叫端提供完整 envelope 與符合 instrument precision profile 的字串 decimal 或原子整數；不接受 provider-specific payload 作為 domain model。
Output contract: 可比較、不可變、可序列化且保留所有時間與 sequence 的 normalized event；拒絕結果必須含固定 reason code。
Failure behavior: null、空 identifier、sequence 非正、時間缺失、scale 不符、overflow、未知 schema/event type 均 fail closed；不猜測或截斷數值。
Idempotency / duplicate behavior: 本 task 僅定義 identity key（source/channel/instrument/event type/sequence 加 payload fingerprint）；duplicate 決策由 T03 執行。
Sequence behavior: sequence 是來源流內單調正整數；不同 source/channel/instrument 不可互相比較。
Time semantics: `sourceTimestamp` 為來源事件時間，`receivedTimestamp` 為本系統接收時間；`processedTimestamp` 由 T03/T04 結果保存，禁止以 `Instant.now()` 補成 source time。
Precision rules: value object 固定 instrument 宣告的 precision/scale；rounding 一律拒絕隱式 rounding，未來若需要 rounding 必須獨立批准；以 decimal plain-string 或 atomic-integer string serialization；檢查 BigDecimal/long overflow boundary。
Concurrency assumptions: event 自身 immutable，可跨 thread 傳遞；stream ordering 不由 value object 保證。
Persistence impact: no persistence；事件契約不得假定資料庫 schema。
Schema changed: no。
Money-impacting: no。
HUMAN_REVIEW_REQUIRED: yes；精度與事件 identity 是後續估值/風控輸入的安全邊界。
Security considerations: source 是受控 enum/value identifier，不接受任意 provider payload 注入；不保存 credential。
Observability requirements: rejection reason、schema version、stream key、sequence、source/received lag 必須可供 T03 指標使用。
Tests required: decimal precision、極端數值/overflow、空/錯誤 instrument、schema version mismatch、完整 identity、immutable payload、serialization round-trip。
Acceptance criteria: contract 不含 provider SDK、float/double、wall-clock identity 或交易核心依賴；所有必填 event 欄位可被測試驗證。
Rollback notes: 新 package 可由單一 revert commit 移除；不變更 legacy DTO 或 schema。
Stop conditions: provider-specific decoding、真實連線、持久化或 public API/WebSocket 要求出現時停止並另開 card。
Documentation updates: README 的 package map、precision/serialization decision note、task status 與 `p21-t02-implementation-review.md`。
Next task: P21-T03；T04/T05 只能依賴已審核的 T02 contract。
```
