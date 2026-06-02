# domain/util

Domain 級工具類。

目前內容：
- Polymarket signing：EIP-712、CLOB auth、order signer、L2 auth signer。
- 安全與資料處理：`SensitiveLogSanitizer`、`PredictionJsonUtils`、`PolymarketResponseSchemaValidator`。
- Market data：`OrderBookChecksum`。
- 其他：`TeamNameParser`。

注意：
- 工具類應保持 stateless。
- 若工具包含安全敏感邏輯，要有測試覆蓋與註解說明輸入/輸出約束。
