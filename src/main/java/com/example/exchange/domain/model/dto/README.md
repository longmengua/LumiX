# domain/model/dto

放「不跟資料表直接連動」的資料模型、讀模型、狀態模型與轉換模型。

白話：
只要不是直接對應資料表，就放這裡。

## 可以放這裡的東西

- 跨層傳遞資料
- 查詢結果
- command result
- read model
- 外部 API schema
- 不直接對表的業務狀態模型
- 交易規則模型
- entity 轉換後給程式使用的模型

例如：

- `Symbol`
- `SymbolConfig`
- `Account`
- `Order`
- `Position`
- `WalletTransfer`
- `DepthDelta`
- `MarketTicker`
- `FundingSettlementResult`
- `LiquidationResult`
- `MatchingResult`

## 關於規則

dto 可以放規則。

例如 `SymbolConfig` 可以放：

- `maxLeverageOrDefault`
- `isPriceAligned`
- `isQtyAligned`
- `riskTierForNotional`

因為它不是資料表 class，而是程式真正拿來判斷交易規則的模型。

## 關於轉換

跨模型轉換放在 dto。

例如：

```text
TradingSymbolRecord + TradingSymbolRiskTierRecord
        ↓
SymbolConfig
```

這種轉換應該放在 `SymbolConfig` 裡，不要放在 `TradingSymbolRecord` 裡。

## 原則

- dto 不直接操作 DB。
- dto 不呼叫 repository。
- dto 不依賴 Spring bean。
- dto 可以承載資料轉換與規則判斷。
- dto 在本專案一律用 Lombok class，不新增 `record`。
- DTO 若需要 JSON 序列化，優先用 `@Data` + `@Builder` + `@Jacksonized`，並保留必要的商業規則方法。
- 若是 record 轉型中的相容期，可以保留和舊呼叫點相同名稱的 accessor，避免一次改壞所有 consumer。
- AI 看到 `domain/model/dto` 時，應假設這裡的標準是 Lombok class，不是 Java `record`。
