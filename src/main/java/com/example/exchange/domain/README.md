# domain

Domain 層，放核心業務模型與不依賴 Spring infrastructure 的規則。

目錄：
- `model/`：entities、DTO、enums。
- `event/`：domain events。
- `repository/`：repository contracts。
- `service/`：domain services 與外部交易域服務。
- `util/`：簽名、checksum、JSON、parser 等工具。

目前狀態：
- 內部交易所與 Polymarket domain 模型共存在此層。
- production 前仍需把部分 MVP model 演進成 durable schema 與 replayable state。
