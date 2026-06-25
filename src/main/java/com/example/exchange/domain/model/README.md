# domain/model

Domain model 聚合目錄。

子目錄：
- `entity/`：可持久化或代表狀態的業務模型。(跟資料庫資料表連動的)
- `dto/`：跨層傳遞的讀模型與結果模型。(不跟資料庫資料表連動的)
- `enums/`：交易方向、訂單型別、margin mode 等列舉。

目前狀態：
- 模型仍偏 MVP，部分 entity 直接存 Redis Java object。
- production schema 應逐步移到明確 DB migration 與序列化版本管理。

## 模型規範

- `domain/model` 內的新模型一律優先用 Lombok class，不用 `record`。
- `dto/` 內的資料結構以 `@Data` + `@Builder` + `@Jacksonized` 為主，必要時保留記憶體內的商業規則方法。
- 既有 record 風格呼叫點的遷移期間，若需要相容，可以保留同名 accessor 方法，但新程式碼仍要以 Lombok class 為準。
- AI 在這個資料夾新增或修改模型時，預設不要提議 `record`，也不要把可變更的業務模型改回 record。
