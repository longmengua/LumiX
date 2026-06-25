# domain/model

Domain model 聚合目錄。

子目錄：
- `entity/`：可持久化或代表狀態的業務模型。(跟資料庫資料表連動的)
- `dto/`：跨層傳遞的讀模型與結果模型。(不跟資料庫資料表連動的)
- `enums/`：交易方向、訂單型別、margin mode 等列舉。

目前狀態：
- 模型仍偏 MVP，部分 entity 直接存 Redis Java object。
- production schema 應逐步移到明確 DB migration 與序列化版本管理。
