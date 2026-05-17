# application

應用層，負責協調 use case、domain service、repository contract 與 infrastructure adapter。

目錄：
- `command/`：use case 輸入模型。
- `usecase/`：對外流程入口，通常由 controller 呼叫。
- `service/`：可被多個 use case 共用的應用服務。
- `scheduler/`：定期任務。
- `event/`：domain event publisher abstraction。

目前狀態：
- 下單、改單、撤單、cancel-replace、資金、風控、對帳、outbox、market data baseline 都在這層協調。
- production 仍需補 durable command log、交易邊界與 worker 拆分。
