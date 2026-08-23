# Phase 21 - Task-card Planning Review

## Required Review Fields

```text
Phase: Phase 21 - Market Data Pipeline
Task: P21-T05 Trade、Ticker 與 Candle Aggregation execution review
Scope: immutable normalized public trade observation、source-time ticker/candle window、OHLCV reducer、late/stale/gap/overflow fail-closed contract；不建立 pipeline runtime
Files changed: `server/src/main/java/com/lumix/marketdata/aggregation/**`、`server/src/test/java/com/lumix/marketdata/aggregation/**`、Phase 21 文件與必要 agent/routing 狀態文件；未修改 legacy market、trading core、schema、web、dependency、production configuration 或 CI/CD
Tests run: P21-T02/T03/T04/T05 contract, policy, projection and aggregation tests、禁止依賴與範圍掃描、`git diff --check`
Test result: P21-T02 至 T05 窄測試通過；完整 server regression 保留既有 architecture gate 狀態文件失敗，未修改禁止範圍掩蓋該問題；詳見 `p21-t05-implementation-review.md`
Schema changed: no
Money-impacting: no
HUMAN_REVIEW_REQUIRED: yes
Human approval status: P21-T01 到 P21-T04 implementation review 已批准；P21-T05 已完成並等待 implementation review；P21-T06 到 P21-T08 仍等待逐卡人類實作批准
Rollback notes: P21-T05 的新 aggregation package 與文件可用新的 revert commit 回復；不得改寫既有 phase review history
Completed task: P21-T05；結論位於 `p21-t05-implementation-review.md`
Next action: 人類審核 P21-T05；僅在收到 P21-T06 的明確人類批准後才可實作後續 task
```

## Repository 現況與 gap analysis

| 區域 | 已有能力 | 限制與缺口 |
| --- | --- | --- |
| `server/com/lumix/market` | Phase 10 `MarketDataService`、`MarkPriceService`、`PriceIndexService`、展示 DTO 與對應 `Default*Service`；`ExternalPriceSource` 僅列舉 provider 名稱 | 無 event contract、sequence、health、aggregation、stream、controller 或 adapter；各 stub 以 `Instant.now()` 產生空或零值 placeholder，provider 名稱不代表外部連線，不能當行情 runtime |
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
Phase 21: formally started; P21-T01 through P21-T04 implementation reviews approved; P21-T05 completed awaiting implementation review
Market Data pipeline runtime implementation: not started; P21-T02/T03/T04/T05 only establish immutable domain, admission-policy and read-only projection/aggregation foundations
Human approval: P21-T05 approved for implementation; P21-T06 through P21-T08 await explicit per-card approval
Production claim: prohibited
```
