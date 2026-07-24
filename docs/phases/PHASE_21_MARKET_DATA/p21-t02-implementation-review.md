# P21-T02 Normalized Market Data Event Contract 實作審核紀錄

## 任務狀態

```text
Task: P21-T02 Normalized Market Data Event Contract
Status: COMPLETED_AWAITING_IMPLEMENTATION_REVIEW
Human implementation approval: received
Scope: provider-neutral、唯讀、immutable domain contract 與 pure serialization mapping
Market Data pipeline runtime: not started
External provider / public API / WebSocket: not started
P21-T03 to P21-T08: awaiting explicit per-card human approval
HUMAN_REVIEW_REQUIRED: yes
Production claim: prohibited
```

## 完成內容與責任邊界

| 區域 | 已完成內容 | 明確不包含 |
| --- | --- | --- |
| `com.lumix.marketdata.contract` | `NormalizedMarketDataEvent`、source/channel/instrument、stream key、sequence、schema version、instrument precision、decimal price、atomic quantity、固定 rejection reason 與 event identity | provider SDK、adapter、clock fallback、duplicate/gap 決策、runtime state |
| sealed payload | immutable `BookSnapshotPayload`、`BookDeltaPayload`、`TradePayload`、`TickerPayload` 與 defensive-copied book levels | order-book projection、matching fill、ticker/candle aggregation |
| `serialization` | domain 與 immutable wire record 的雙向 mapping；價格採 decimal plain-string、數量採 atomic-integer string、時間採 ISO-8601 instant | JSON/REST/WebSocket codec、database、cache、broker、producer/consumer |
| contract tests / fixtures | 固定 timestamp、precision、identity、payload mismatch、overflow、unknown schema/type、immutability 與 round-trip 驗證 | 外部 provider、secret、真實網路連線 |

## 核心決策與不變式

1. event envelope 完整保存 `source`、`channel`、`instrumentId`、`eventType`、`sequence`、`sourceTimestamp`、`receivedTimestamp`、`schemaVersion` 與 payload，另保存 immutable `InstrumentPrecision`，不新增 `processedTimestamp`。
2. identity 為 `source/channel/instrument/eventType/sequence/payload fingerprint`；fingerprint 由固定 canonical payload form 的 SHA-256 產生，不含 received time、本機時間或隨機值。duplicate 決策仍屬 P21-T03。
3. `Sequence` 只能在 `StreamKey(source, channel, instrument)` 內比較且必須為正整數；T02 不建立排序、gap、resync 或 health runtime。
4. `DecimalPrice` 只接受符合 price scale 的 plain-string decimal；`AtomicQuantity` 只接受 canonical atomic-integer string。scale mismatch、scientific notation、前導零、overflow 或任何隱式 rounding 一律以固定 reason code 拒絕。
5. payload 為 sealed、book list defensive copy，故 event 可安全跨 thread 傳遞；sealed boundary 阻止 provider-specific payload 直接作為 domain payload。
6. 未知 schema version、未知 event type、缺少時間、不完整 stream、payload/type mismatch 與無效數值全部 fail closed；沒有相容性猜測或輸入自動修正。

## 驗證證據

```text
./server/mvnw -Dtest=NormalizedMarketDataEventContractTest,MarketDataPrecisionContractTest,MarketDataPayloadImmutabilityTest,NormalizedMarketDataEventWireMapperTest test
Result: 12 tests, 0 failures, 0 errors

./server/mvnw test
Result: 312 tests, 3 failures, 0 errors, 2 skipped
```

完整 regression 的 3 個失敗均為既有 P15/P16/P17 architecture gate（`P15T09Phase15FinalReviewGateTest`、
`P16T10SpotSandboxFinalReviewGateTest`、`P17T05Phase17FinalReviewGateTest`）硬性期待 `AI_PROGRESS.md`
仍含已過時的 `Phase 21: PLANNED_NOT_STARTED`。目前權威狀態必須如實記錄 P21-T01 已批准、P21-T02
等待 implementation review，故不以回填錯誤狀態或修改禁止範圍的既有 architecture test 來掩蓋失敗。
此 regression gate 的更新需另行審核；P21-T02 contract tests 本身全數通過。

`git diff --check`、禁止依賴／禁止檔案掃描與 serialization deterministic fixture 檢查將在提交前再次執行。

## No-claim 與下一步

本 task 沒有建立 market-data pipeline runtime、external provider adapter、API、WebSocket、projection、aggregation、persistence、cache 或 event broker；更沒有改動 matching、trade/fill、position、balance、ledger、reservation、settlement 或 wallet。它不表示正式行情服務、公開行情、real-time SLA、正式交易或 production-ready。

P21-T02 完成後只能等待 implementation review。P21-T03 必須另有經審核 task card 與人類明確批准，且不得把本 contract 直接接到 provider、transport 或交易核心。
