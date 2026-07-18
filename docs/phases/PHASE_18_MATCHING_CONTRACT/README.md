# Phase 18 - Futures Trading Sandbox

## 狀態

```text
in progress — T01-T04 completed
```

## 目標

建立受限 futures trading sandbox。這是加速路線中最早可以做受限合約交易 sandbox 的 phase，但仍不是 production。

資料夾歷史名稱仍為 `PHASE_18_MATCHING_CONTRACT`，但本 phase 的正式名稱是 `Phase 18 - Futures Trading Sandbox`；
T01 只完成 futures order placement，不代表 matching 已開始。

## Sandbox 內容

```text
futures order placement
matching reuse
position update
realized / unrealized PnL
mock mark price
mock funding
restricted contract trade sandbox
```

## 不在 phase

```text
single market public launch
real money
public users
production trading
```

## 高層 task list

```text
T01 futures order placement - completed
T02 matching reuse - completed (pure candidate evaluation only)
T03 position update - completed (one-way, open-only sandbox snapshots)
T04 realized / unrealized PnL - completed (pure valuation and close preview only)
T05 mock mark price and funding - not started
T06 restricted contract sandbox gate - not started
```

## Sandbox 限制

```text
P18 最早只能做受限 futures sandbox，不能視為正式合約交易。
單一 market、mock price only、sandbox only、不可接真錢、不可開放 public users。
accepted 不代表 matched
accepted 不代表 filled
accepted 不代表 position opened
accepted 不代表 margin reserved
accepted 不代表 balance frozen
accepted 不代表 ledger posted
accepted 不代表 settled
```

## HUMAN_REVIEW_REQUIRED

```text
任何 futures / PnL / mark price / funding runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把受限 contract sandbox 誤寫成正式合約交易上線的行為都屬於 HUMAN_REVIEW_REQUIRED。
```

## T01 implementation notes

- Scope: 只建立 `com.lumix.trading.core.futures.order` 的 sandbox order placement request / result / gate 與 accepted order snapshot，不接 order book、matching、trade、position update、reservation、ledger 或 settlement runtime。
- Package placement: `FuturesOrderId`、`FuturesOrderSide`、`FuturesOrderType`、`FuturesTimeInForce`、`FuturesOrderStatus`、`FuturesSandboxOrder`、`FuturesOrderPlacementRequest`、`FuturesOrderPlacementReason`、`FuturesOrderPlacementResult`、`FuturesOrderPlacementGate` 都放在 `com.lumix.trading.core.futures.order`。
- Supported order type: 目前只支援 `LIMIT`。這是 type boundary，不透過 runtime reason 表達 unsupported values。
- Supported time-in-force: 目前只支援 `GTC`。這是 type boundary，不透過 runtime reason 表達 unsupported values。
- Order side vs position side: order side 使用 `BUY / SELL`；position side 仍是 Phase 17 的 `LONG / SHORT`。T01 不負責把 order side 推導成最終 position transition。
- Margin proposal consistency design: placement request 攜帶原始 `IsolatedMarginCheckRequest`。placement gate 不信任外部 approved result，而是使用 Phase 17 的 pure deterministic margin gate 重新計算；order 的 account、market、quantity、price 與 leverage config 必須和 margin proposal 完全一致，否則拒絕。
- Placement evaluation order: `account consistency -> market consistency -> leverage-config equality -> quantity/price proposal consistency -> recomputed margin approval -> accepted sandbox snapshot creation`。
- Accepted semantics: accepted 只代表 sandbox placement gate 接受這筆 order，並建立 immutable accepted snapshot；acceptedAt 直接沿用 submittedAt，避免引入 clock dependency。
- Accepted snapshot design: accepted snapshot 只包含 order identity、request identity、account、market、side、type、quantity、limit price、time-in-force、已驗證 leverage、acceptedAt、status 與 optional clientOrderId；不包含 fill、trade、position、reservation、ledger、settlement 或 order-book sequence。
- Rejection semantics:
  - `ACCOUNT_MISMATCH`：order account 與 margin proposal account 不一致。
  - `MARKET_MISMATCH`：order market 與 margin proposal market 不一致。
  - `MARGIN_PROPOSAL_MISMATCH`：quantity、price 或 leverage config 與 margin proposal 不一致。
  - `MARGIN_CHECK_NOT_APPROVED`：重算後的 margin result 不是 `APPROVED / SUFFICIENT_MARGIN`，包含 internal mismatch rejection、insufficient margin 與其他 non-approved margin outcome。
- Validation commands and result:
  - `./mvnw -q -Dtest=FuturesOrderIdTest,FuturesOrderPlacementRequestTest,FuturesOrderPlacementResultTest,FuturesOrderPlacementGateTest test`
  - `./mvnw -q -Dtest=FuturesAccountTest,FuturesPositionTest,FuturesMarketSymbolTest,FuturesLeverageTest,IsolatedLeverageConfigTest,IsolatedMarginCheckRequestTest,IsolatedMarginCheckResultTest,IsolatedMarginCheckGateTest,FuturesOrderIdTest,FuturesOrderPlacementRequestTest,FuturesOrderPlacementResultTest,FuturesOrderPlacementGateTest test`
  - `./mvnw test`
- Sandbox limitations: T01 沒有 matching、fill、trade、position update、PnL、mark price、funding、reservation、ledger、wallet、settlement、persistence、API、Spring runtime、public-user 或 real-money capability。

## T02 implementation notes

- Scope: 抽出 spot 與 futures 共用的 pure、stateless 限價單候選配對規則；spot matcher 改用該規則維持既有的價格、時間與 maker priority。Futures 只把 `ACCEPTED_FOR_SANDBOX` immutable snapshots 轉為 candidate evaluation，不建立 order book、不產生 trade / fill、不更新 position、PnL、reservation、ledger 或 settlement。
- Shared policy: `com.lumix.trading.core.sandbox.matching.SandboxLimitOrderMatchingPolicy` 只選出單一 market 的最佳 crossed pair：BUY 高價優先、SELL 低價優先、同價位依 `acceptedAt` 再依 order ID 排序，`matchedQuantity = min(buyRemaining, sellRemaining)`；不持有狀態、不迴圈撮合。
- Futures boundary: `com.lumix.trading.core.futures.matching.FuturesSandboxMatchingGate` 要求輸入同一 market 且 order ID 不重複，並回傳 `MATCH_ELIGIBLE`、`NO_CROSS` 或 `REJECTED`。`MATCH_ELIGIBLE` 只表示候選限制價格交叉，絕不等於 matched、filled、trade created 或 position opened。
- Reuse safety: futures adapter 不依賴 spot order book、spot trade/fill result 或任何 persistence / transaction runtime；spot runtime 保留在原 package，只改以 shared pure policy 選擇下一組候選。
- Validation commands and result:
  - `./mvnw -q -Dtest=SandboxLimitOrderMatchingPolicyTest,FuturesSandboxMatchingGateTest,SpotSandboxMatchingRuntimeBoundaryTest,SpotSandboxTradeFillBoundaryTest test`
  - `./mvnw test`
- Sandbox limitations: T02 不是 futures matching engine execution；沒有 futures order-book storage、state mutation、fill、trade、position update、PnL、mark price、funding、reservation、ledger、wallet、settlement、persistence、API、Spring runtime、public-user 或 real-money capability。

## T03 implementation notes

- Scope: 建立 `com.lumix.trading.core.futures.position.update` 的 immutable verified-fill input 與 pure position opening gate。T03 是 one-way、open-only；只回傳買方 LONG 與賣方 SHORT 的 sandbox snapshots，不修改既有 position。
- Verified fill boundary: `FuturesSandboxVerifiedFill` 是 T03 的唯一 position opening 依據，必須包含不同帳戶的 BUY / SELL accepted orders、同一 market、雙方限價內的 fill price、不得超過雙方原始 quantity 的 fill quantity，以及不早於接受時間的 `filledAt`。不得從 T02 candidate 直接更新 position；T03 也不提供 candidate-to-fill 或 fill producer。
- One-way and open-only rule: BUY 固定建立 LONG，SELL 固定建立 SHORT；任一方已存在同 account / market 的 OPEN position 即拒絕。加倉、減倉、平倉、反轉、hedge mode、平均 entry price 與 realized PnL 都不在本 task。
- Idempotency contract: request 明確帶入不可變的 `processedFillIds`；相同 fill ID 已存在時回傳 `FILL_ALREADY_PROCESSED`。pure gate 不接 idempotency store，後續 runtime 必須在 adapter 層實際保證此集合的正確性。
- Snapshot semantics: position ID 由 `fillId + buy/sell` deterministic 產生；entry price 與 quantity 直接沿用 verified fill，`openedAt / updatedAt` 均使用 `filledAt`，避免額外 clock dependency。
- Validation commands and result:
  - `./mvnw -q -Dtest=FuturesSandboxVerifiedFillTest,FuturesSandboxPositionUpdateGateTest,FuturesPositionTest,FuturesSandboxMatchingGateTest test`
  - `./mvnw test`
- Sandbox limitations: T03 不產生 fill、不執行 matching、不持久化 position、不更新 balance、不保留 margin、不寫 ledger、不計算 realized / unrealized PnL、不接 mark price、funding、settlement、API、Spring runtime、public-user 或 real-money capability。

## T04 implementation notes

- Scope: 建立 `com.lumix.trading.core.futures.pnl` 的 pure、stateless PnL calculator。unrealized PnL 接收呼叫端提供的單次 mark price；realized PnL 只提供不改變 position 的 close preview，不建立 close fill、closed position、settlement 或任何帳務異動。
- Calculation: LONG 使用 `(price - entryPrice) * quantity`，SHORT 使用 `(entryPrice - price) * quantity`。unrealized 使用 position 全量與 mark price；realized preview 使用明確 close price / close quantity，且 close quantity 不得大於 position quantity。全程使用 `BigDecimal` 與 `MoneyAmount`，不做除法、rounding 或 scale policy。
- Asset and ownership boundary: request 必須攜帶擁有 position 的 `FuturesAccount`，calculator 以其 settlement asset 標記結果；帳戶不屬於 position 時直接拒絕，避免把 PnL 計入錯誤資產或帳戶。
- Price boundary: `FuturesSandboxPnlPrice` 只是一個正數 valuation input value object，不提供 mark-price source。T05 才負責 mock mark price / funding；T04 不依賴既有正式 mark-price service。
- Close-preview semantics: realized PnL 只提供不改變 position 的 close preview；不會產生 trade、fill、position close / partial close、平均 entry price、fee、funding、maintenance margin、liquidation 或 balance / ledger side effect。
- Validation commands and result:
  - `./mvnw -q -Dtest=FuturesSandboxPnlCalculatorTest,FuturesPositionTest,FuturesAccountTest test`
  - `./mvnw test`
- Sandbox limitations: T04 不提供 mark-price provider、不執行 matching、不產生或持久化 fill / trade / position close、不更新 balance、不保留 margin、不寫 ledger、不處理 fee、funding、liquidation、settlement、API、Spring runtime、public-user 或 real-money capability。
