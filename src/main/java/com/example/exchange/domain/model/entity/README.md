# domain/model/entity

只放「跟資料表連動」的業務資料模型。

白話：
這裡的 class 基本上就是資料表對應物，通常會有：

- `@Entity`
- `@Table`
- `@Id`
- `@Column`

例如：

- `TradingSymbolRecord`
- `TradingSymbolRiskTierRecord`
- `OutboxEventRecord`
- `WalletLedgerEntryRecord`
- `MatchingCommandLogRecord`
- `PredictionMarketInfo`

## 不放這裡的東西

不跟資料表直接連動的模型，不放這裡。

例如：

- `Symbol`
- `SymbolConfig`
- `Account`
- `Order`
- `Position`
- `WalletTransfer`

這些放在：

```text
domain/model/dto
```

## 原則

- entity 只描述資料表欄位。
- entity 不放複雜商業規則。
- entity 不直接呼叫 repository。
- entity 不處理跨模型轉換。
- 跨模型轉換放在 dto。
- entity 也維持 Lombok class 風格，不使用 `record`。
- 若只是在 entity 上省樣板，優先用 `@Getter` / `@Setter` / `@Data` 等 Lombok 註解，而不是改成 record。
