# Phase 17 - Futures Core Model

## 狀態

```text
in progress
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
T04 margin check gate - pending
T05 no-formal-trading review - pending
```

## Sandbox 限制

```text
這一階段只建立 futures core model，不能宣稱可正式交易。
futures / margin runtime 仍屬 HUMAN_REVIEW_REQUIRED。
```

## HUMAN_REVIEW_REQUIRED

```text
任何 futures / margin runtime 變更都屬於 HUMAN_REVIEW_REQUIRED。
任何把 isolated margin 誤寫成完整 futures trading 的行為都屬於 HUMAN_REVIEW_REQUIRED。
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
