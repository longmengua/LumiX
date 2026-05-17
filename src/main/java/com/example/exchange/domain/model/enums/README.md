# domain/model/enums

Domain enum 集中目錄。

目前內容：
- 內部交易所：`OrderSide`、`OrderType`、`TimeInForce`、`MarginMode`。
- Polymarket：CLOB side、direction、order type 等。

注意：
- enum 名稱會影響 REST JSON 與持久化資料，修改前要確認 backward compatibility。
- 若外部 API enum 可能變動，優先在 parser/normalizer 做兼容，而不是直接改 domain enum。
