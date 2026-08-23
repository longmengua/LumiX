# P21-T05 Trade、Ticker 與 Candle Aggregation 實作審核紀錄

## 任務狀態

```text
Task: P21-T05 Trade、Ticker 與 Candle Aggregation
Status: COMPLETED_AWAITING_IMPLEMENTATION_REVIEW
Human implementation approval: received
Scope: immutable normalized public trade observation、source-time ticker/candle aggregation 與 pure reducer
Market Data pipeline runtime: not started
External provider / public API / WebSocket / persistence: not started
P21-T06 to P21-T08: awaiting explicit per-card human approval
HUMAN_REVIEW_REQUIRED: yes
Production claim: prohibited
```

## 實作方向與核心不變式

| 區域 | 已完成內容 | 明確不包含 |
| --- | --- | --- |
| `com.lumix.marketdata.aggregation` | immutable trade observation、24h ticker window、固定 1m/5m/1h UTC candle、OHLCV 與 quote-volume value model | trade/fill producer、matching、PnL、funding、position 或任何交易 mutation |
| reducer | 僅接受 T03 `ACCEPTED` 且 `HEALTHY` 的 `TRADES` stream；驗證 stream、event identity、連續 sequence 與 source time | shared cursor map、wall clock、provider connection、cache、DB、broker 或 transport |
| source time | candle 採 `[start, end)` UTC bucket；ticker 以最新 accepted trade 的來源時間回推 24h；late event 一律拒絕不回填 | received time 重分桶或自行猜測市場時間 |
| numeric boundary | price 與 base volume 沿用 T02 `DecimalPrice`/`AtomicQuantity`；quote volume 以 unscaled integer 與明確 scale 相乘/累加 | float/double、隱式 rounding、超限截斷 |
| resource boundary | ticker window 固定最多 1,024 個 trade；超限保留既有 projection 並降級 | 無界歷史 trade/candle state |

`AggregationStatus` 會明確暴露 `UNAVAILABLE`、`HEALTHY`、`STALE`、`GAP_DETECTED`、`RESYNC_REQUIRED` 或 `DEGRADED`。只有 `HEALTHY` 表示 reducer 未偵測到本 task 範圍內的完整性問題；它不代表正式行情、成交執行真實性、real-time SLA 或 production-ready。

空 projection 沒有 ticker/candle，也不會填入零價格或零成交量。duplicate、gap、stale、非 trade stream、admission mismatch、identity 重複、late source-time event、ticker 上限與 numeric overflow 都不會修改已發布的 OHLCV、ticker 或 as-of metadata。

## 驗證證據

```text
cd server && ./mvnw -Dtest=ReadOnlyTradeTickerCandleReducerTest,NormalizedMarketDataEventContractTest,MarketDataPrecisionContractTest,MarketDataPayloadImmutabilityTest,NormalizedMarketDataEventWireMapperTest,MarketDataStreamAdmissionPolicyTest,ReadOnlyOrderBookProjectionReducerTest test
Result: 43 tests, 0 failures, 0 errors, 0 skipped
```

P21-T05 固定 fixture 覆蓋：正常 OHLCV/ticker、source-time window、duplicate、gap、stale、late event、24h eviction、multi-instrument isolation、deterministic replay、quote-volume overflow 與 1,024 筆 ticker window 上限。禁止依賴掃描未發現 trading/ledger/balance/position/reservation/settlement、provider、transport、persistence、`float` 或 `double` 運算依賴；唯一 `double` 字樣位於禁止使用 binary floating point 的說明註解。

完整 regression 仍會有既有 P15/P16/P17 architecture gate 的三個失敗：它們硬性期待 `AI_PROGRESS.md` 含過時的 `Phase 21: PLANNED_NOT_STARTED`。P21-T05 不修改其禁止範圍的 legacy architecture test，也不以回填錯誤狀態掩蓋該問題；此 gate 更新需另行審核。

`git diff --check` 已通過。

## No-claim 與下一步

本 task 沒有建立 market-data pipeline runtime、外部 provider adapter、公開 API/WebSocket、persistence、cache、event broker、shared projection store，也沒有改動 matching、trade/fill、position、balance、ledger、reservation、settlement 或 wallet。normalized public trade observation 不等同 LumiX fill 或交易成交真實性。

P21-T05 完成後只能等待 implementation review。P21-T06（deterministic replay、resync 與 recovery boundary）需另有經審核 task card 與人類明確 implementation approval；在此之前不得把此 reducer 接到 provider、transport、matching 或任何交易核心。
