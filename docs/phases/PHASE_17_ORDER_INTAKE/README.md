# Phase 17 - Futures Core Model

## 狀態

```text
completed
```

## 目標

建立 futures core model 的 sandbox 前置層，只做 isolated margin、position model、leverage config 與 margin check。

## Core model 內容

```text
futures account model
isolated margin only
position model
leverage config
margin check
```

## 不在 phase

```text
cross margin
liquidation runtime
funding runtime
formal futures trading
```

## 高層 task list

```text
T01 futures account model - completed
T02 isolated margin position model - completed
T03 leverage config model - completed
T04 margin check gate - completed
T05 no-formal-trading review - completed
```

## Sandbox 限制

```text
這一階段只建立 futures core model，不能宣稱可正式交易。
futures / margin runtime 仍屬 HUMAN_REVIEW_REQUIRED。
no real money
no order intake
no matching
no settlement
no ledger mutation
no balance reservation
no liquidation
no funding
```

## HUMAN_REVIEW_REQUIRED

```text
任何 futures / margin runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把 isolated margin 誤寫成完整 futures trading 的行為都屬於 HUMAN_REVIEW_REQUIRED。
Phase 17 人工審核完成。
本輪 Phase 17 scope 的 HUMAN_REVIEW_REQUIRED 已完成審核關閉。
後續任何 futures / margin runtime 新變更仍需重新標記 HUMAN_REVIEW_REQUIRED。
```

## T01 implementation notes

- Scope: 只建立 `com.lumix.trading.core.futures.account` 的 sandbox immutable domain model，不接 position、leverage、margin calculation、matching、settlement 或 ledger mutation。
- Model decisions: `FuturesAccount` 重用既有 `AccountId`、`UserId`、`AccountStatus`、`AssetSymbol`，並以 `FuturesMarginMode.ISOLATED` 固定目前唯一可用的 margin mode。
- Invariants: 必填欄位不可為 null，`updatedAt` 不可早於 `createdAt`，`marginMode` 不可離開 isolated。
- Constructor semantics: canonical constructor 是正式驗證邊界，`open(...)` 只是建立新 ACTIVE isolated account 的 convenience factory，不是唯一建立路徑。
- Verification: 已新增單元測試覆蓋 convenience factory、canonical constructor 直接建構、margin mode 封鎖、timestamp chronology 與 null input guard。
- Known limits: 這只是 sandbox domain model，沒有 position、leverage、margin check、liquidation、funding、futures order execution，也沒有任何 production trading API。
- HUMAN_REVIEW_REQUIRED: 所有 futures / margin 相關變更仍保留人工審核前提，Phase 17 不代表正式 futures runtime 已啟用。

## T02 implementation notes

- Scope: 只建立 `com.lumix.trading.core.futures.position` 的 sandbox immutable position model，不接 cross margin、margin calculation、liquidation、funding、matching、settlement 或 persistence。
- Model decisions: `FuturesPosition` 以 `FuturesPositionId`、`AccountId`、`FuturesMarketSymbol`、`FuturesPositionSide`、`FuturesPositionQuantity`、`FuturesEntryPrice`、`FuturesPositionStatus`、`openedAt` 與 `updatedAt` 表達最小完整狀態。
- Reused value objects: `AccountId` 直接重用既有 shared account identity；`FuturesMarketSymbol` 則是 futures-specific identity value object，避免把 `TradingSymbol` 這種展示模型直接嵌入 position。
- New value objects: `FuturesPositionId`、`FuturesMarketSymbol`、`FuturesPositionQuantity`、`FuturesEntryPrice`，原因是現有 shared model 沒有能精準表達 position identity、futures market identity 與 position 專用正數 magnitude 的型別。
- Invariants: 必填欄位不可為 null，`quantity` 與 `entryPrice` 必須大於 0，`side` 只能是 LONG / SHORT，`status` 目前只允許 OPEN，`updatedAt` 不可早於 `openedAt`，模型不可表達 cross margin pooling。
- Validation commands: `./mvnw -q -Dtest=FuturesAccountTest,FuturesPositionTest,FuturesMarketSymbolTest test`，以及 `./mvnw test`。
- Sandbox limitations: 不包含 initial margin、maintenance margin、available margin、margin ratio、unrealized PnL、realized PnL、liquidation price、bankruptcy price、leverage calculation、mark price 或 funding fee；目前也不包含 position close / partial close state transition。
- HUMAN_REVIEW_REQUIRED: 所有 futures / margin 相關變更仍保留人工審核前提，Phase 17 不代表正式 futures runtime 已啟用。

## T03 implementation notes

- Scope: 只建立 `com.lumix.trading.core.futures.leverage` 的 sandbox leverage value object 與 isolated leverage config model，不接 margin check、margin calculation、liquidation、funding、matching、settlement 或 persistence。
- Package placement: `FuturesLeverage` 與 `IsolatedLeverageConfig` 都放在 `com.lumix.trading.core.futures.leverage`，避免把 leverage 規則混進 account 或 position package。
- Model decisions: `FuturesLeverage` 使用正整數 `multiplier` 表達槓桿，`IsolatedLeverageConfig` 以 `AccountId + FuturesMarketSymbol` 作為 ownership 邊界，並持有 `FuturesLeverage`、`createdAt`、`updatedAt`。
- Reused value objects: `AccountId`、`FuturesMarketSymbol`、`FuturesLeverage`；沒有重複建立 account ID、market symbol 或 leverage 的變體。
- Invariants: leverage 必須為正整數，config 的 account / market / leverage / timestamps 不可為 null，`updatedAt` 不可早於 `createdAt`，`reconfigure(...)` 的 `changedAt` 不可早於目前 `updatedAt`。
- Configure semantics: `configure(...)` 是 convenience factory，只建立新的 snapshot，且 `createdAt == updatedAt`。
- Reconfigure semantics: `reconfigure(...)` 回傳新 snapshot，不修改原物件，保留 account / market / createdAt，只更新 leverage 與 updatedAt。
- Same-leverage behavior: 相同 leverage 重新設定仍會建立新的 immutable snapshot，時間會更新，因為這代表可審計的 reconfigure 事件，不是 no-op。
- Validation commands: `./mvnw -q -Dtest=FuturesAccountTest,FuturesPositionTest,FuturesMarketSymbolTest,FuturesLeverageTest,IsolatedLeverageConfigTest test`，以及 `./mvnw test`。
- Sandbox limitations: 目前沒有硬編碼最大槓桿倍數，也沒有 initial margin、maintenance margin、available margin、margin ratio、PnL、liquidation price、funding fee、cross margin 或 hedge mode。
- HUMAN_REVIEW_REQUIRED: 所有 futures / margin 相關變更仍保留人工審核前提，Phase 17 不代表正式 futures runtime 已啟用。

## T04 implementation notes

- Scope: 只建立 `com.lumix.trading.core.futures.margin` 的 pure in-memory isolated initial-margin sufficiency gate，不接 balance lookup、reservation、order intake、position creation、matching、settlement、liquidation、funding、ledger mutation 或 persistence。
- Package placement: `IsolatedMarginCheckRequest`、`FuturesMarginCheckStatus`、`FuturesMarginCheckReason`、`IsolatedMarginCheckResult`、`IsolatedMarginCheckGate` 都放在 `com.lumix.trading.core.futures.margin`，避免把 margin gate 規則分散到 account、position 或 leverage package。
- Runtime shape: gate 保持 pure、stateless、deterministic、thread-safe by immutability，沒有 Spring dependency、I/O、clock、database 或 external service dependency。
- Request inputs: `FuturesAccount`、`IsolatedLeverageConfig`、`FuturesMarketSymbol`、`FuturesPositionQuantity`、`FuturesEntryPrice`、`AssetSymbol availableMarginAsset`、`MoneyAmount availableMargin`。
- Request invariants: 所有欄位不可為 null，`availableMargin` 不可為負數，但可為 0；`quantity` 與 `entryPrice` 沿用既有 value object 的正數 invariant；request 不接受 `AccountBalanceView`、repository、service 或裸 `BigDecimal` quantity / price。
- Account consistency checks: account status 必須是 `ACTIVE`，否則回傳 `ACCOUNT_NOT_ACTIVE`；`leverageConfig.futuresAccountId` 必須等於 `futuresAccount.accountId`，否則回傳 `ACCOUNT_MISMATCH`。
- Market consistency checks: `request.marketSymbol` 必須等於 `leverageConfig.marketSymbol`，否則回傳 `MARKET_MISMATCH`。
- Settlement asset consistency checks: `request.availableMarginAsset` 必須等於 `futuresAccount.settlementAsset`，否則回傳 `SETTLEMENT_ASSET_MISMATCH`；不允許拿其他 asset 的可用餘額充當 settlement margin，也不形成 cross-margin pooling。
- Evaluation order: `account status -> account identity match -> market symbol match -> settlement asset match -> requested notional calculation -> supported notional calculation -> sufficiency comparison`。
- Exact comparison formula: `requestedNotional = quantity × entryPrice`；`marginSupportedNotional = availableMargin × leverageMultiplier`；若 `marginSupportedNotional >= requestedNotional` 則 `APPROVED`，否則 `REJECTED / INSUFFICIENT_MARGIN`。
- Why multiplication instead of division: 在正數條件下，`availableMargin × leverage >= requestedNotional` 與 initial-margin sufficiency 判斷等價；它可避免在 settlement asset precision 與 rounding policy 尚未定義前先做除法，因此不會偷偷引入 scale、MathContext 或四捨五入爭議；這也再次說明 T04 不是正式交易所的完整 margin engine。
- Equality boundary: 等值邊界採 `compareTo` semantics，只要 `marginSupportedNotional` 與 `requestedNotional` 數值相等就 `APPROVED`，不受 trailing zero 影響。
- Rejection reasons: `ACCOUNT_NOT_ACTIVE`、`ACCOUNT_MISMATCH`、`MARKET_MISMATCH`、`SETTLEMENT_ASSET_MISMATCH`、`INSUFFICIENT_MARGIN`。
- Result design: `APPROVED` 只能搭配 `SUFFICIENT_MARGIN`；`INSUFFICIENT_MARGIN` rejection 會攜帶 `requestedNotional` 與 `marginSupportedNotional`；account / market / asset / status mismatch rejection 則不攜帶計算值，改用 `Optional.empty()` 明確表示「未計算」，避免用 `null` 或偽造數字混淆結果。
- Rounding policy: 本題沒有 rounding policy；不使用除法、不使用 `setScale`、不注入 `MathContext`、不偷偷四捨五入。
- Maximum leverage policy: 本題沒有 hard-coded maximum leverage policy。
- Fee buffer policy: 本題沒有 fee buffer，也不包含手續費、滑價或 funding reserve。
- Validation commands and result:
  - `./mvnw -q -Dtest=IsolatedMarginCheckRequestTest,IsolatedMarginCheckResultTest,IsolatedMarginCheckGateTest test`：passed
  - `./mvnw -q -Dtest=FuturesAccountTest,FuturesPositionTest,FuturesMarketSymbolTest,FuturesLeverageTest,IsolatedLeverageConfigTest,IsolatedMarginCheckRequestTest,IsolatedMarginCheckResultTest,IsolatedMarginCheckGateTest test`：passed
  - `./mvnw test`：passed
- Sandbox limitations: 這個 gate 不代表帳戶真實餘額已查核，不代表資金已被保留，不代表 order 已被接受，不代表 position 已被建立，也不代表 futures trading 可用；目前仍不包含 maintenance margin、margin ratio、liquidation、bankruptcy price、mark price、index price、PnL、funding、cross margin、hedge mode、portfolio margin、wallet mutation 或任何正式 runtime。
- HUMAN_REVIEW_REQUIRED: 所有 futures / margin 相關變更仍保留人工審核前提；T04 只完成 sandbox capacity gate，不代表正式 futures runtime 已啟用。

## T05 final review notes

- Review result: T01-T04 的 scope、immutable model 邊界、BigDecimal 計算與 no-formal-trading 限制已完成實作層、測試層與人工審核。
- Final status wording: `Phase 17: COMPLETED`，並記錄 `Phase 17 human review: APPROVED` 與 `Phase 17 人工審核完成`。
- Final review document: `docs/phases/PHASE_17_ORDER_INTAKE/phase-17-final-review.md`。
- No-formal-trading audit: Phase 17 只完成 sandbox core model 與 pure margin capacity gate；沒有 futures order intake、matching、settlement、ledger mutation、balance reservation、liquidation、funding、API、persistence、Spring runtime 或 real-money capability。
- Human approval result: 已收到 `Phase 17 人工審核完成`，因此本輪可將 Phase 17 更新為 completed；但這不代表 Phase 18 已開始。
