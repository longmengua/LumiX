# Phase 21 - Task-card Planning Review

## Required Review Fields

```text
Phase: Phase 21 - Market Data Pipeline
Task: Phase planning and task-card definition
Scope: provider-neutral、唯讀 market-data foundation 的施工邊界、8 張任務卡、依賴順序與 no-claim gate；未實作 runtime
Files changed: docs/phases/PHASE_21_MARKET_DATA/ task cards / review、AI status and routing entry files
Tests run: git diff --check；文件一致性人工檢查
Test result: passed — `git diff --check` and task-card/document consistency checks passed; backend tests not run because this is a documentation-only planning task with no runtime code changed
Schema changed: no
Money-impacting: no
HUMAN_REVIEW_REQUIRED: yes
Human approval status: awaiting review
Rollback notes: 本輪只有文件；以新的 revert commit 回復本輪文件即可，不得改寫既有 phase review history
Proposed first task: P21-T01
Next action: 人類審核 task cards；僅在收到「批准 P21-T01，依已審核 task card 開始實作。」後才可實作 P21-T01
```

## Repository 現況與 gap analysis

| 區域 | 已有能力 | 限制與缺口 |
| --- | --- | --- |
| `server/com/lumix/market` | Phase 10 `MarketDataService`、展示 DTO、空 `DefaultMarketDataService` | 無 event contract、sequence、health、aggregation、stream、controller 或外部來源；stub 以 `Instant.now()` 回空 book，不能當行情 runtime |
| `server/trading/core/spot/orderbook` | sandbox in-memory order book | 保存 sandbox orders，與外部/normalized market data 無關，禁止重用為 P21 projection 或行情來源 |
| futures sandbox mark price | 人工輸入的 immutable mock valuation snapshot | 只供 sandbox PnL gate，非行情服務、非 provider adapter |
| `web/features/trading/mockTradingService.ts` | 合成 order book、trade tape、K 線與 ticker 版面資料 | 使用 `number`、`Date.now()`、seeded fixture；僅 UI/mock，不能接 production path 或作為 domain contract |
| `web/features/markets/mockMarketService.ts` | 固定市場清單與 ticker 卡片 | 純前端 fixture，無 sequence、健康狀態、精度或來源語意 |
| OpenAPI registry | `/depth`、`/trades`、`/ticker`、`/kline` metadata | 僅 route metadata，沒有 controller/handler；P21-T07 不會實作公開 transport |

缺失的核心 domain contract 為 provider-neutral envelope、instrument scale/rounding、三種時間、stream sequence/duplicate/gap、feed health、snapshot/delta 契約、immutable projection/aggregation、deterministic replay/resync 及內部 consumer backpressure。

## 與 Phase 20 的銜接

Phase 20 是受限的 contract trading integration gate，明確未啟動 matching/fill、position/balance/ledger mutation 或 settlement。Phase 21 只接續其 no-claim 與 sandbox isolation 原則：行情資料只形成唯讀輸入投影，不能反向驅動 Phase 20 或更早 phase 的交易、帳本與資金狀態。

## Task cards 與審核順序

```text
P21-T01 boundary/invariants
  -> P21-T02 normalized contract
  -> P21-T03 sequence/health policy
  -> P21-T04 book projection
  -> P21-T05 trade/ticker/candle aggregation
  -> P21-T06 replay/resync boundary
  -> P21-T07 internal query/stream backpressure
  -> P21-T08 integration/no-claim final review
```

T04 與 T05 都依賴 T02/T03，施工時可在各自獲批准後獨立進行；T06 等待兩者。每張卡均可單獨執行、單獨驗收與單獨 rollback。

## 不會啟動的 runtime

本輪及擬議 task cards 不會啟動 matching、internal order intake、trade/fill producer、position/balance/ledger/reservation/settlement/wallet mutation。也不會接正式 provider、API key、secret、production endpoint、公開 API 或 WebSocket。若未來需要 Binance、OKX、Bybit 或其他 adapter，必須另開 provider-specific task card，記錄 license/usage、rate limit、snapshot/delta、sequence、reconnect/resubscription、checksum、fixture、secret handling 與 failure isolation。

## HUMAN_REVIEW_REQUIRED

所有 P21 task cards 均為 `HUMAN_REVIEW_REQUIRED: yes`。雖無直接 money mutation，行情完整性、精度與降級策略是後續風控/估值可能依賴的安全輸入；人類應逐卡審核其 fail-closed 行為。尤其 T02（精度/identity）、T03（gap/stale）、T04（book authority）、T05（時間/聚合）、T06（recovery）與 T07（consumer loss visibility）不得在未批准下實作。

## 目前結論

```text
Phase 21 task cards: drafted
Runtime implementation: not started
Human approval: awaiting review
Production claim: prohibited
```
