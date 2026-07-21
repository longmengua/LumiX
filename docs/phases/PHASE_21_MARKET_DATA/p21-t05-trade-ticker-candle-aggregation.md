# P21-T05 Trade、Ticker 與 Candle Aggregation

```text
Task ID: P21-T05
Task name: Trade、Ticker 與 Candle Aggregation
Status: PROPOSED_AWAITING_HUMAN_REVIEW
Objective: 以 normalized trade events 建立唯讀 ticker 與 candle aggregation，並固定來源事件時間的窗口語意。
Why this task exists: 現有 `Ticker24hView`、`KlineView` 是未實作展示模型，前端 K 線與 ticker 是 number-based mock，沒有可審計 window 或來源時間契約。
Prerequisites: P21-T02 and P21-T03 implemented and human-reviewed；本 task card 已獲 implementation approval。
Dependencies: P21-T02, P21-T03。
Scope: trade projection、window policy、ticker state、OHLCV candle reducer、late/out-of-window behavior、immutable query model 與 fixtures。
Out of scope: trade/fill producer、matching、mark price/PNL 更新、funding、持久化、public API/WebSocket、UI adapter replacement。
Deliverables: read-only aggregators、固定 interval/window contract、precision-safe arithmetic、tests 與 aggregation decision note。
Allowed files: server/src/main/java/com/lumix/marketdata/**、server/src/test/java/com/lumix/marketdata/**、docs/phases/PHASE_21_MARKET_DATA/。
Forbidden files: trading/matching/fill producer、futures PnL/position、ledger/balance/reservation/settlement、migration、web/src/、provider connection。
Domain model: normalized public trade observation、ticker window state、candle interval、OHLCV、window key、aggregation health/as-of metadata。
Core invariants: aggregation 只接受 T03 accepted trade event；ticker/candle window 都用 sourceTimestamp；同一 trade duplicate 不改總量；無 trade 的 window 不虛構 price/volume。
Input contract: T02 trade payload、T03 decision、明確 interval/window policy 與 instrument precision profile。
Output contract: immutable trade/ticker/candle view，附 source window start/end、as-of sequence、feed health；不得聲稱 trade execution truth。
Failure behavior: unknown instrument/schema、負/零價格或數量、late event、window conflict、overflow、非 healthy feed 均回傳 reason/status；不得以 received time 重新分桶。
Idempotency / duplicate behavior: duplicate trade identity 不重複計入 OHLCV/ticker；identity conflict 由 T03 降級，聚合不修補。
Sequence behavior: 只消費連續 accepted stream；gap/resync-required 時凍結或標示 degraded，不能繼續標為 complete window。
Time semantics: sourceTimestamp 決定 candle open/close window 和 24h ticker rolling window；receivedTimestamp 用於 freshness；processedTimestamp 僅可觀測。
Precision rules: price、base volume、quote volume 使用 T02 value object；quote-volume 乘法需明示 scale、overflow 與 reject-on-unrepresentable policy；percent 必須有明確 scale/rounding，禁止 double。
Concurrency assumptions: 同一 instrument/aggregation window 順序化；跨 instrument 可平行；輸出 immutable，collection ordering 需固定。
Persistence impact: none；不寫歷史 trade/candle 表。
Schema changed: no。
Money-impacting: no。
HUMAN_REVIEW_REQUIRED: yes；價格聚合可影響後續顯示與估值理解，需審查 source time、精度與 degraded 表達。
Security considerations: 對 interval、window 數與 trade payload 設上限，避免無界 state。
Observability requirements: accepted/rejected trade、window close、late event、volume overflow、ticker/candle health、source-to-received lag。
Tests required: 正常序列、duplicate、out-of-order/gap 冻结、source-time window、late trade、precision/極端數值、空 window、多 instrument、deterministic replay。
Acceptance criteria: 相同 fixture 得到相同 OHLCV/ticker；mock number 與 `Date.now()` 不進 domain；沒有 execution/PNL mutation。
Rollback notes: revert aggregator package；無 schema/state migration。
Stop conditions: 任一需求將 trade 視為 LumiX fill、改 position/balance 或接外部 provider 時停止並另建 task。
Documentation updates: ticker/candle window 與 late-event policy、task status。
Next task: P21-T06；P21-T07 需等待 T04、T05、T06。
```
