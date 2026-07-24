# P21-T01 現況、邊界與不變式審核紀錄

## 任務狀態

```text
Task: P21-T01 現況盤點、領域邊界與核心不變式
Status: COMPLETED_AWAITING_IMPLEMENTATION_REVIEW
Human implementation approval: received
Scope: documentation-only inventory and boundary definition
Market Data runtime: not started
P21-T02 to P21-T08: awaiting explicit per-card human approval
HUMAN_REVIEW_REQUIRED: yes
Production claim: prohibited
```

## 現況與缺口表

| 區域 | 已盤點能力 | 目前限制 | P21 邊界結論與缺口 |
| --- | --- | --- | --- |
| `server/src/main/java/com/lumix/market` | `MarketDataService`、`MarkPriceService`、`PriceIndexService`、展示用 DTO 與對應 `Default*Service`；另有 `ExternalPriceQuote`／`ExternalPriceSource` 模型 | `getDepth()`、mark price 與 price index 都以 `Instant.now()` 產生 placeholder；depth/trade/ticker/kline 回傳空結果，price index 回傳零值，外部來源 enum 僅是名稱列舉，沒有 adapter 或外部連線；沒有 event envelope、sequence、health、aggregation、stream 或 controller | 此 package 是 legacy stub boundary；enum 中的 provider 名稱不代表已授權或已連線。它不能作為 authoritative market data，T02 前不得重用其 DTO 作 normalized contract |
| `server/src/main/java/com/lumix/trading/core/spot/orderbook` | sandbox in-memory order record、idempotency 與 sandbox matching 狀態 | 內容來自 sandbox order intake，並由 sandbox matching 更新 remaining quantity/status；非外部行情、非 snapshot/delta | 禁止成為 P21 行情來源、projection 儲存體或 replay 輸入 |
| `server/src/main/java/com/lumix/trading/core/futures/sandbox/market` | `FuturesSandboxMockMarkPrice` immutable valuation snapshot | 僅允許 `MANUAL_SANDBOX_INPUT`；價格與發布時間由測試或受限 sandbox 呼叫端明確帶入 | 只供 sandbox PnL／風控試算；不是 provider adapter、price index 或 authoritative mark price |
| `web/src/features/trading/mockTradingService.ts` | 合成 order book、trade tape、ticker、K 線、部位與餘額版面資料 | 使用 `number`、`Date.now()`／`new Date()`、本地計算與固定/seeded fixture；同時含 UI-only order/position/balance | 僅限前端展示 mock；不得接 production path，亦不得作 domain contract、事件來源或權威 order book |
| `web/src/features/markets/mockMarketService.ts` | 固定市場清單與 ticker 卡片 | 使用 `number` 與靜態 fixture；沒有 source、sequence、時間、精度、health 或 resync 語意 | 僅限前端 fixture；不得宣稱或轉接為行情服務 |
| `server/src/main/java/com/lumix/openapi/OpenApiRouteRegistry.java` | `/depth`、`/trades`、`/ticker`、`/kline`、`/mark-price` route metadata | 註解明示僅 metadata；沒有 controller、handler、transport contract 或公開服務 | T01 不實作 API；T07 即使獲批准也只處理內部 query/stream contract，公開 transport 需獨立 task card |
| `com.lumix.common.MoneyAmount` | `BigDecimal` 值物件，避免 binary floating point | 沒有 instrument-specific scale、rounding、overflow 或 serialization contract | T02 必須建立獨立、明確的 instrument precision contract；不可把 `MoneyAmount` 或 UI `number` 直接當成答案 |

## 唯讀資料領域邊界

```text
後續批准的 provider adapter
            |
            v
provider-neutral normalized event（T02；唯讀輸入）
            |
            v
sequence / health policy（T03）
            |
     +------+------+
     |             |
     v             v
book projection   trade/ticker/candle aggregation
（T04）           （T05）
     |             |
     +------+------+
            |
            v
replay / resync（T06） -> internal query / stream（T07）

禁止反向依賴或資料流：
market data -> matching / trade-fill producer / order / position / balance /
               ledger / reservation / settlement / wallet

禁止作為來源或儲存體：
legacy market stub、spot sandbox order book、futures sandbox mock valuation、
frontend mock、local clock
```

行情資料領域只消費後續 task 明確定義且已正規化的唯讀事件，並只能產生唯讀 projection、aggregation、health 與內部 consumer 可讀結果。它不授權任何交易或資金核心狀態改變。

## 後續 package、責任 owner 與禁止依賴

此表是 P21 後續 task 的命名與責任分配；不建立 package、類別或 runtime。

| 後續 package | 唯一責任 owner | 最早 task | 禁止依賴 |
| --- | --- | --- | --- |
| `com.lumix.marketdata.contract` | provider-neutral normalized event、instrument precision、schema version 與事件 identity | P21-T02 | `trading.core.*`、`ledger.*`、`wallet.*`、web mock、OpenAPI transport、provider-specific SDK |
| `com.lumix.marketdata.health` | 同一 instrument-stream 的 sequence、duplicate、gap、stale、resync 與 health state | P21-T03 | matching/fill、position/balance、wall-clock 作 event identity、跨 stream 全域 sequence |
| `com.lumix.marketdata.book` | 由相容 snapshot/delta 建立唯讀 order-book projection | P21-T04 | `trading.core.spot.orderbook`、order intake、matching state、trade-only/ticker/UI mock 來源 |
| `com.lumix.marketdata.aggregate` | 從 normalized trade event 產生 trade、ticker、candle 唯讀聚合 | P21-T05 | frontend `number`、本機接收時間作 candle window、position/PnL/fee runtime |
| `com.lumix.marketdata.replay` | 固定排序事件的 deterministic replay、recovery 與 resync boundary | P21-T06 | 隨機值、未固定 collection ordering、隱性系統 clock、交易/帳本 mutation |
| `com.lumix.marketdata.query` | 僅供內部使用的唯讀 query/stream contract、consumer lag 與 backpressure 可見性 | P21-T07 | public controller/WebSocket、order placement/cancel、任何資金或交易 mutation |

`com.lumix.market` 保留為 legacy stub boundary；除非未來有獨立且經批准的 migration task，後續 P21 package 不得以它承載 authoritative event、projection 或 transport。任何 external provider adapter、公開 API/WebSocket、secret 或 production endpoint 都不在上述 package map，必須另有 provider-/transport-specific task card 與人類批准。

## 固定核心不變式

1. 行情資料為 provider-neutral、唯讀輸入與唯讀結果；不得直接或間接修改 matching、trade/fill、position、balance、ledger、reservation、settlement 或 wallet。
2. 價格、數量、成交量與衍生數值不得使用 `float`、`double` 或前端 `number` 作 authoritative value。T02 必須對每個 instrument 固定 scale、rounding、overflow 行為與字串 serialization；現有 `MoneyAmount(BigDecimal)` 不構成完整 instrument precision contract。
3. 每個 normalized event 至少必須有 `source`、`channel`、`instrumentId`、`eventType`、`sequence`、`sourceTimestamp`、`receivedTimestamp`、`schemaVersion` 與 payload。事件 identity 不能由 `Instant.now()`、`Date.now()` 或處理當下時間建立。
4. sequence 只在相同 `(source, channel, instrumentId)` stream key 中比較。duplicate 必須冪等；out-of-order 必須拒絕或隔離；gap 必須可見並進入 `GAP_DETECTED`／`RESYNC_REQUIRED`，不得靜默標示健康。
5. 必須區分來源事件時間、系統接收時間與處理完成時間。candle/ticker window 只以來源事件時間計算；接收時間只可量測 lag/stale；處理完成時間只供處理證據，不可改寫事件時間。
6. health 僅可為 `HEALTHY`、`STALE`、`GAP_DETECTED`、`RESYNC_REQUIRED`、`DEGRADED` 或 `STOPPED`。非 `HEALTHY` 必須能被下游辨識，且不得把 stale/degraded 資料呈現為即時正常行情。
7. 給定相同初始狀態與同一排序事件序列，projection、aggregation 與 health state 必須相同。禁止 wall clock、隨機值、隱性外部 I/O 或非決定性 collection ordering 影響結果；replay 必須保留 source/channel/instrument、sequence、lag、health transition 與 resync evidence。
8. authoritative order book 只可由相容 snapshot 加連續 delta 建立。sandbox order book、trade-only feed、ticker、futures mock valuation 與 UI mock 皆不是 authoritative book；缺少必要契約時必須 fail closed，拒絕建立虛假的 book。
9. Phase 21 完成任何 foundation 仍不表示正式行情服務、公開 API/WebSocket、外部 provider 連線、real-time SLA、正式交易或 production-ready。P21-T01 更未建立任何 runtime。

## 可供 implementation review 檢查的證據

| 檢查項目 | 結果 |
| --- | --- |
| 僅修改 task card 允許的文件範圍 | 是；未修改 `server/src/`、`web/src/`、migration、dependency、production config 或 CI/CD |
| market stub、sandbox book、futures mock valuation、前端 mock、OpenAPI metadata 已分別盤點 | 是 |
| package owner 與禁止依賴已明確 | 是；僅定義未來 package map，未建立 runtime |
| precision、time、sequence、health、replay 與 no-claim 已固定 | 是 |
| Market Data runtime、provider adapter、API、WebSocket、projection、aggregation | 未開始 |
| P21-T02 到 P21-T08 | 未批准、未執行 |

## Rollback 與下一步

本 task 僅變更文件；若 implementation review 否決，必須以新的 revert commit 回復本 task 文件，不得改寫既有 phase review history。下一步只能是人類審核本紀錄；P21-T02 必須等待其 task card 的明確人類實作批准。
